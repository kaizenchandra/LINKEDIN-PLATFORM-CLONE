'use client';
import {useEffect,useRef,useState} from 'react';
import SiteHeader from '../site-header';
import {useLiveRefresh} from '../use-live-refresh';
import {messagingFetch as authenticatedFetch,MessagingSessionExpired,MessagingTimeout} from '../messaging-api';
import type {components} from '../api-schema';
type Notification=components['schemas']['Notification'];
type Preference=components['schemas']['NotificationPreference'];
export default function Notifications(){
 const [name,setName]=useState<string>(),[items,setItems]=useState<Notification[]>([]),[cursor,setCursor]=useState<string|null>(null),[unread,setUnread]=useState(0),[message,setMessage]=useState(''),[busy,setBusy]=useState(false),[preference,setPreference]=useState<Preference|null>(null);
 const [readThrough,setReadThrough]=useState<string|null>(null);
 const [category,setCategory]=useState<'ALL'|'NETWORK'|'JOBS'>('ALL');
 const [before,setBefore]=useState<string>(),[ready,setReady]=useState(false),[expired,setExpired]=useState(false),[error,setError]=useState('');
 const active=useRef<AbortController|null>(null),generation=useRef(0),stopped=useRef(false),writing=useRef(false);
 function cancel(){generation.current++;active.current?.abort();active.current=null}
 function expire(){stopped.current=true;cancel();setExpired(true);setItems([]);setCursor(null);setUnread(0);setReadThrough(null);setPreference(null);setName(undefined);setMessage('');setError('')}
 function fail(cause:unknown){if(cause instanceof MessagingSessionExpired)expire();else setError(cause instanceof MessagingTimeout?'Updates timed out. Refresh or wait for recovery.':cause instanceof TypeError?'Connection interrupted. Retrying automatically.':(cause as Error).message)}
 async function sync(){
  if(stopped.current||active.current||writing.current)return true;
  const controller=new AbortController();active.current=controller;const version=generation.current;
  try{
   const responses=await Promise.all(['/api/v1/notifications?category='+category+(before?'&before='+encodeURIComponent(before):''),'/api/v1/notifications/preferences','/api/v1/me'].map(url=>authenticatedFetch(url,{cache:'no-store',signal:controller.signal})));
   if(responses.some(response=>!response.ok))throw Error('Updates unavailable. Refresh or wait for recovery.');
   const [data,pref,me]=await Promise.all(responses.map(response=>response.json())) as [components['schemas']['NotificationPage'],Preference,{name:string}];
   if(version!==generation.current)return true;
   setItems(data.items);setCursor(data.nextCursor);setUnread(data.unreadCount);setReadThrough(data.readThrough);setPreference(pref);setName(me.name);setReady(true);setError('');return true;
  }catch(cause){if(version===generation.current&&!controller.signal.aborted)fail(cause);return false}
  finally{if(active.current===controller)active.current=null}
 }
 const realtime=useLiveRefresh(sync,expired);
 useEffect(()=>{let disposed=false,timer:ReturnType<typeof setTimeout>,delay=3000;
  const tick=async()=>{const ok=await sync();delay=ok?3000:Math.min(delay*2,30000);if(!disposed&&!stopped.current)timer=setTimeout(tick,delay)};
  void tick();return()=>{disposed=true;clearTimeout(timer);cancel()};
 },[before,category]);
 function navigate(next:string|undefined){cancel();setItems([]);setCursor(null);setReady(false);setBefore(next);setMessage('');setError('')}
 async function write(url:string,body?:unknown,method:'PUT'|'DELETE'='PUT'){
  const response=await authenticatedFetch('/api/v1/csrf');if(!response.ok)throw Error('Unable to verify this request. Retry.');const csrf=await response.json();
  const result=await authenticatedFetch(url,{method,headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:body===undefined?undefined:JSON.stringify(body)});
  if(!result.ok)throw Error(result.status===409?'Preferences changed elsewhere. Reload before retrying.':'Change could not be confirmed. Refresh and retry.');return result;
 }
 async function mutate(url:string,body:unknown,success:string,method:'PUT'|'DELETE'='PUT'){
  if(writing.current||stopped.current)return;cancel();writing.current=true;setBusy(true);setError('');setMessage('');
  try{await write(url,body,method);writing.current=false;await sync();if(!stopped.current)setMessage(success)}catch(cause){fail(cause)}finally{writing.current=false;setBusy(false)}
 }
 async function mark(item:Notification){await mutate(`/api/v1/notifications/${item.id}/read`,undefined,item.read?'Notification marked unread.':'Notification marked read.',item.read?'DELETE':'PUT')}
 async function dismiss(item:Notification){if(window.confirm('Dismiss this notification? It will be removed from your list.'))await mutate(`/api/v1/notifications/${item.id}`,undefined,'Notification dismissed.','DELETE')}
 async function change(enabled:boolean,jobs:boolean){if(preference)await mutate('/api/v1/notifications/preferences',{enabled,jobs,version:preference.version},'Notification preferences saved.')}
 return <><SiteHeader name={name}/><main id="main" className="feed-layout notifications-layout"><aside className="feed-left" aria-label="Account navigation"><section className="sidebar-card"><h2>{name??'Your account'}</h2><p><a href="/">View my profile</a></p><a href="/network">Manage my network</a></section></aside><div className="feed-column"><section className="profile-card composer-card"><h1>Notifications</h1><p>Unread: {unread}</p><div className="hero-actions notification-categories" aria-label="Notification categories">{([["ALL","All"],["NETWORK","Network"],["JOBS","Jobs"]] as const).map(([value,label])=><button key={value} className="outline" disabled={busy||expired} aria-pressed={category===value} onClick={()=>{navigate(undefined);setCategory(value)}}>{label}</button>)}</div>{before&&<p className="muted">Viewing older notifications. Return to newest for recent arrivals.</p>}<p className="muted">Connection and job application updates.</p><button className="outline" disabled={busy||expired} onClick={()=>void sync()}>Refresh notifications</button><button className="outline" disabled={busy||expired||!readThrough||!unread} onClick={()=>void mutate('/api/v1/notifications/read',{through:readThrough},'Notifications marked read.')}>Mark all read</button></section><p className="muted" aria-live="polite">{realtime}</p><p role="status">{expired?'Session expired. Sign in again.':error||(!ready?'Loading notifications…':message)}</p>{ready&&!error&&!expired&&!items.length&&<section className="profile-card"><h2>You’re all caught up</h2><p>New updates in this category will appear here.</p></section>}{items.map(item=><article key={item.id} className={'profile-card notification-card '+(item.read?'':'unread')} aria-label={`Notification from ${item.actorName}`}><strong>{item.actorName}</strong>{item.kind==='CONNECTION_ACCEPTED'?<p>accepted your connection invitation.</p>:<><p>{item.jobTitle}</p><p>Application {({SUBMITTED:"submitted",REVIEWING:"moved to review",ACCEPTED:"accepted",REJECTED:"rejected",WITHDRAWN:"withdrawn"})[item.applicationStatus!]}. This records an update; open the application for its current status.</p></>}<p className="muted"><time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString()}</time> · {item.read?'Read':'Unread'}</p><div className="hero-actions">{item.kind==='CONNECTION_ACCEPTED'?<a className="button outline" href="/network">View network</a>:<a className="button outline" href={'/applications?view='+(['SUBMITTED','WITHDRAWN'].includes(item.applicationStatus!)?'RECEIVED':'MINE')+'&job='+item.jobId}>View application</a>}<button className="outline" disabled={busy||expired} onClick={()=>void mark(item)}>{item.read?'Mark unread':'Mark read'}</button><button className="text-button" disabled={busy||expired} onClick={()=>void dismiss(item)}>Dismiss</button></div></article>)}{cursor&&<button className="outline" disabled={busy||expired} onClick={()=>navigate(cursor)}>Older notifications</button>}{before&&<button className="outline" disabled={busy||expired} onClick={()=>navigate(undefined)}>Newest notifications</button>}</div><aside className="feed-right" aria-label="Notification preferences"><section className="sidebar-card"><h2>Manage notifications</h2><p className="muted">Choose which new in-app updates to receive. Previously delivered updates remain.</p><label><input type="checkbox" checked={preference?.enabled??true} disabled={busy||expired||!preference} onChange={event=>void change(event.target.checked,preference?.jobs??true)}/> New connection notifications</label><label><input type="checkbox" checked={preference?.jobs??true} disabled={busy||expired||!preference} onChange={event=>void change(preference?.enabled??true,event.target.checked)}/> Job application notifications</label><button className="outline" disabled={busy||expired} onClick={()=>void sync()}>Reload notification preferences</button></section></aside></main></>;
}
