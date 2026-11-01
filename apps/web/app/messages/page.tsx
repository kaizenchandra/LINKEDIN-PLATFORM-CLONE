'use client';
import {FormEvent,useEffect,useRef,useState} from 'react';
import SiteHeader,{Icon} from '../site-header';
import {useLiveRefresh} from '../use-live-refresh';
import {messagingWrite,messagingFetch,MessagingSessionExpired,MessagingTimeout} from '../messaging-api';
import type {components} from '../api-schema';
type Conversation=components['schemas']['Conversation'];
type Message=components['schemas']['Message'];
type History=components['schemas']['MessagePage'];
export default function Messaging(){
 const [name,setName]=useState<string>(),[conversations,setConversations]=useState<Conversation[]>([]),[nextConversations,setNextConversations]=useState<string|null>(null),[listBefore,setListBefore]=useState<string>();
 const [view,setView]=useState<'INBOX'|'ARCHIVED'|'STARRED'>('INBOX');
 const [query,setQuery]=useState(''),[searchDraft,setSearchDraft]=useState(''),[listReady,setListReady]=useState(false);
 const organizing=useRef(false);
 const [sessionExpired,setSessionExpired]=useState(false);
 const [selected,setSelected]=useState<string|null>(null),[history,setHistory]=useState<History|null>(null),[messages,setMessages]=useState<Message[]>([]),[older,setOlder]=useState<string|null>(null);
 const [draft,setDraft]=useState(''),[busy,setBusy]=useState(false),[status,setStatus]=useState('Loading conversations…'),[error,setError]=useState('');
 const current=useRef<string|null>(null),draftRef=useRef(''),sendId=useRef<string|null>(null),readThrough=useRef(0),activeRefresh=useRef<AbortController|null>(null),generation=useRef(0),expired=useRef(false);
 useEffect(()=>{draftRef.current=draft},[draft]);
 useEffect(()=>{const warn=(event:BeforeUnloadEvent)=>{if(draftRef.current.trim())event.preventDefault()};window.addEventListener('beforeunload',warn);return()=>window.removeEventListener('beforeunload',warn)},[]);
 useEffect(()=>{void messagingFetch('/api/v1/me').then(async response=>{const me=response.ok?await response.json():null;if(me&&!expired.current)setName(me.name)}).catch(cause=>{if(cause instanceof MessagingSessionExpired)expire()});
  const id=new URLSearchParams(window.location.search).get('conversation');if(id){current.current=id;setSelected(id)}
 },[]);
 function cancelRefresh(){generation.current++;activeRefresh.current?.abort();activeRefresh.current=null}
 function expire(){expired.current=true;setSessionExpired(true);cancelRefresh();current.current=null;readThrough.current=0;sendId.current=null;draftRef.current='';setSelected(null);setMessages([]);setHistory(null);setOlder(null);setConversations([]);setListReady(false);setQuery('');setSearchDraft('');setNextConversations(null);setDraft('');setName(undefined);setError('');setStatus('Session expired. Sign in again.')}
 async function sync(){
  if(expired.current||activeRefresh.current||organizing.current)return true;
  const controller=new AbortController();activeRefresh.current=controller;
  const version=generation.current,id=current.current;
  try{
   const list=await messagingFetch('/api/v1/conversations?view='+view+'&q='+encodeURIComponent(query)+(listBefore?'&before='+listBefore:''),{cache:'no-store',signal:controller.signal});if(!list.ok)throw Error('Updates unavailable. Retrying automatically.');
   const data:components['schemas']['ConversationPage']=await list.json();if(version!==generation.current)return true;
   setConversations(data.items);setNextConversations(data.nextCursor);setListReady(true);
   if(id){
    const result=await messagingFetch('/api/v1/conversations/'+encodeURIComponent(id)+'/messages',{cache:'no-store',signal:controller.signal});
    if(!result.ok){if(version===generation.current){setMessages([]);setHistory(null);setOlder(null)}throw Error(result.status===404?'Conversation unavailable. It may be blocked.':'Updates unavailable. Retrying automatically.')}
    const page:History=await result.json();if(version!==generation.current)return true;setHistory(page);
    setMessages(previous=>{
     const first=page.items[0]?.sequence,last=previous.at(-1)?.sequence;
     if(!previous.length||(first!==undefined&&last!==undefined&&first>last+1)){setOlder(page.nextCursor);return page.items}
     const merged=new Map(previous.map(item=>[item.id,item]));for(const item of page.items)merged.set(item.id,item);
     return [...merged.values()].map(item=>item.owned?{...item,read:item.sequence<=page.recipientReadSequence}:item).sort((a,b)=>a.sequence-b.sequence);
    });
    if(document.visibilityState==='visible'&&page.latestSequence>readThrough.current){
     await messagingWrite('/api/v1/conversations/'+id+'/read','PUT',{throughSequence:page.latestSequence},controller.signal);
     if(version===generation.current)readThrough.current=page.latestSequence;
    }
   }
   if(version===generation.current)setStatus('Updates every 3 seconds.');return true;
  }catch(cause){
   if(version===generation.current){
    if(cause instanceof MessagingSessionExpired)expire();
    else if(!controller.signal.aborted)setStatus(cause instanceof MessagingTimeout?'Updates timed out. Retrying automatically.':cause instanceof TypeError?'Connection interrupted. Retrying automatically.':(cause as Error).message);
   }return false;
  }finally{if(activeRefresh.current===controller)activeRefresh.current=null}
 }
 const realtime=useLiveRefresh(sync,sessionExpired);
 useEffect(()=>{let cancelled=false,timer:ReturnType<typeof setTimeout>,delay=3000;
  const tick=async()=>{const ok=await sync();delay=ok?3000:Math.min(delay*2,30000);if(!cancelled&&!expired.current)timer=setTimeout(tick,delay)};
  void tick();return()=>{cancelled=true;clearTimeout(timer);cancelRefresh()};
 },[selected,listBefore,view,query]);
 function choose(id:string){if(busy||id===selected)return;if(draft.trim()&&!window.confirm('Discard this unsent message?'))return;cancelRefresh();current.current=id;readThrough.current=0;sendId.current=null;setSelected(id);setHistory(null);setMessages([]);setOlder(null);setDraft('');setError('')}
 async function send(event:FormEvent){event.preventDefault();if(!selected||!draft.trim()||busy)return;setBusy(true);setError('');sendId.current??=crypto.randomUUID();
  try{await messagingWrite('/api/v1/conversations/'+selected+'/messages/'+sendId.current,'PUT',{text:draft});sendId.current=null;setDraft('');draftRef.current='';await sync()}catch(cause){if(cause instanceof MessagingSessionExpired)expire();else setError((cause as Error).message)}finally{setBusy(false)}
 }
 async function organize(starred:boolean,archived:boolean){
  if(!selected||!history||busy)return;cancelRefresh();const version=generation.current;
  organizing.current=true;setBusy(true);setError('');
  try{
   const result=await messagingWrite('/api/v1/conversations/'+selected+'/preferences','PUT',{starred,archived,version:history.preferences.version});
   const preferences:components['schemas']['ConversationPreference']=await result.json();
   if(version===generation.current)setHistory(value=>value?{...value,preferences}:value);
  }catch(cause){if(cause instanceof MessagingSessionExpired)expire();else setError((cause as Error).message)}
  finally{organizing.current=false;setBusy(false)}
  await sync();
 }
 function changeView(next:typeof view){if(busy||next===view)return;cancelRefresh();setListReady(false);setConversations([]);setNextConversations(null);setListBefore(undefined);setView(next)}
 function searchConversations(next:string){if(busy)return;const normalized=next.trim();cancelRefresh();setConversations([]);setNextConversations(null);setListReady(false);setStatus('Updating conversations…');setListBefore(undefined);setQuery(normalized);if(normalized===query&&!listBefore)void sync()}
 async function loadOlder(){if(!selected||!older)return;setBusy(true);const version=generation.current;try{
  const result=await messagingFetch('/api/v1/conversations/'+selected+'/messages?before='+older,{cache:'no-store'});if(!result.ok)throw Error('Older messages unavailable. Refresh to retry.');const page:History=await result.json();
  if(version===generation.current){setMessages(previous=>{const merged=new Map([...page.items,...previous].map(item=>[item.id,item]));return [...merged.values()].sort((a,b)=>a.sequence-b.sequence)});setOlder(page.nextCursor);setError('')}
 }catch(cause){if(cause instanceof MessagingSessionExpired)expire();else setError((cause as Error).message)}finally{setBusy(false)}}
 return <><SiteHeader name={name}/><main id="main" className="messaging-layout"><section className="conversation-list profile-card" aria-label="Conversations"><h1>Messaging</h1><p className="muted">Direct messages with your connections.</p><p className="muted" aria-live="polite">{realtime}</p><form className="conversation-search" onSubmit={event=>{event.preventDefault();searchConversations(searchDraft)}}><label>Find conversations by name<input maxLength={120} value={searchDraft} disabled={busy||expired.current} onChange={event=>setSearchDraft(event.target.value)}/></label><button className="outline" disabled={busy||expired.current}>Search conversations</button>{query&&<button className="outline" type="button" disabled={busy} onClick={()=>{setSearchDraft('');searchConversations('')}}>Clear search</button>}</form><div className="conversation-filters" aria-label="Conversation view">{(['INBOX','STARRED','ARCHIVED'] as const).map(option=><button key={option} className="outline" aria-pressed={view===option} disabled={busy} onClick={()=>changeView(option)}>{option==='INBOX'?'Inbox':option==='STARRED'?'Starred':'Archived'}</button>)}</div><button className="outline" disabled={busy} onClick={()=>void sync()}>Refresh messages</button>{!listReady&&!expired.current&&<p>Loading conversations…</p>}{listReady&&conversations.length===0&&<p>{query?'No conversations match this name in the selected view.':'No conversations in this view. Start one from an accepted connection’s profile, or choose another view.'}</p>}{conversations.map(item=><button className="conversation-choice" key={item.id} aria-pressed={selected===item.id} disabled={busy} onClick={()=>choose(item.id)}><strong>{item.name}</strong><span>{item.unreadCount} unread</span>{item.preferences.starred&&<span>Starred</span>}{item.preferences.archived&&<span>Archived</span>}{!item.canSend&&<span>History only</span>}</button>)}{nextConversations&&<button disabled={busy} className="outline" onClick={()=>{cancelRefresh();setListBefore(nextConversations)}}>Older conversations</button>}{listBefore&&<button disabled={busy} className="outline" onClick={()=>{cancelRefresh();setListBefore(undefined)}}>Newest conversations</button>}</section><section className="message-thread profile-card" aria-label="Conversation"><p role="status">{status}</p>{history?<><header><h2>{history.name}</h2><a href={'/members/'+history.memberId}>View member profile</a><div className="conversation-actions"><button className="outline" aria-pressed={history.preferences.starred} disabled={busy} onClick={()=>void organize(!history.preferences.starred,history.preferences.archived)}>{history.preferences.starred?'Unstar conversation':'Star conversation'}</button><button className="outline" disabled={busy} onClick={()=>void organize(history.preferences.starred,!history.preferences.archived)}>{history.preferences.archived?'Restore to inbox':'Archive conversation'}</button></div><p className="muted">{history.preferences.archived?'Archived for you. New messages return this conversation to your inbox.':'Stars and archives are private to your account.'}</p></header>{older&&<button className="outline" disabled={busy} onClick={()=>void loadOlder()}>Load older messages</button>}<div className="message-history" aria-label="Message history">{messages.map(item=><article className={'message-bubble '+(item.owned?'sent':'received')} key={item.id} aria-label={item.owned?'Sent message':'Received message'}><p>{item.text}</p><small><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString()}</time>{item.owned?' · '+(item.read?'Read':'Sent'):''}</small></article>)}</div>{!messages.length&&<p>No messages yet. Start the conversation.</p>}{history.canSend?<form onSubmit={send}><label>Message<textarea maxLength={3000} required rows={3} value={draft} disabled={busy} onChange={event=>{setDraft(event.target.value);sendId.current=null}}/></label><button disabled={busy||!draft.trim()}>Send message</button></form>:<p>Sending is unavailable. An accepted connection is required.</p>}</>:<div className="empty-conversation"><span className="empty-conversation-icon"><Icon name="message"/></span><h2>Your conversations start here</h2><p>Select a conversation, or start one from an accepted connection’s profile.</p><a className="button outline" href="/network">Find a connection</a></div>}<p role="alert">{error}</p></section><aside className="messaging-sidebar" aria-label="Messaging resources"><section className="sidebar-card"><h2>Your conversations</h2><p className="muted">Keep in touch with the people in your professional network.</p><a className="button outline" href="/network">View connections</a></section><section className="sidebar-card"><h2>Make a new connection</h2><p className="muted">Find colleagues and professionals who share your interests.</p><a href="/search">Find people →</a></section><footer className="sidebar-footer"><a href="/settings">Settings &amp; privacy</a><span>Meridian · Professional community</span></footer></aside></main></>;
}
