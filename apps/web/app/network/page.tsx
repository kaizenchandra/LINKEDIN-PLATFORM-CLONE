'use client';
import {useEffect,useRef,useState} from 'react';
import SiteHeader,{Icon} from '../site-header';
import Following from '../following';
import {Followers,NetworkDiscovery} from '../network-discovery';
import Appearance from '../appearance';
import {messagingFetch,MessagingSessionExpired} from '../messaging-api';
import type {components} from '../api-schema';
type Connection=components['schemas']['Connection'];
export default function NetworkPage(){
 const [name,setName]=useState<string>(),[items,setItems]=useState<Connection[]>([]),[view,setView]=useState('invitations'),[query,setQuery]=useState(''),[busy,setBusy]=useState(true),[expired,setExpired]=useState(false),[message,setMessage]=useState('Loading network…');
 const active=useRef<AbortController|null>(null);
 function fail(error:unknown){if(error instanceof MessagingSessionExpired){setExpired(true);setItems([]);setName(undefined)}setMessage((error as Error).message)}
 async function load(){
  active.current?.abort();const controller=new AbortController();active.current=controller;setBusy(true);
  try{const [network,me]=await Promise.all([messagingFetch('/api/v1/network',{cache:'no-store',signal:controller.signal}),messagingFetch('/api/v1/me',{cache:'no-store',signal:controller.signal})]);if(!network.ok||!me.ok)throw Error('Network unavailable. Refresh to retry.');
   const rows=await network.json(),account=await me.json();if(!controller.signal.aborted){setItems(rows);setName(account.name);setMessage('')}
  }catch(error){if(!controller.signal.aborted)fail(error)}finally{if(!controller.signal.aborted)setBusy(false)}
 }
 useEffect(()=>{void load();return()=>active.current?.abort()},[]);
 async function act(item:Connection,action:string){
  if(busy||expired)return;if(action==='REMOVE'&&!window.confirm('Remove connection with '+item.name+'?'))return;setBusy(true);
  try{const token=await messagingFetch('/api/v1/csrf');if(!token.ok)throw Error('Unable to verify request.');const csrf=await token.json();
   const response=await messagingFetch('/api/v1/network/'+item.id,{method:'PUT',headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify({action,version:item.version})});
   if(!response.ok)throw Error('Request changed or unavailable. Refresh your network before retrying.');await load();setMessage('Network updated.');
  }catch(error){fail(error)}finally{setBusy(false)}
 }
 const accepted=items.filter(i=>i.state==='ACCEPTED'),incoming=items.filter(i=>i.state==='PENDING'&&i.incoming),sent=items.filter(i=>i.state==='PENDING'&&!i.incoming);
 const rows=(view==='connections'?accepted:view==='sent'?sent:incoming).filter(i=>i.name.toLowerCase().includes(query.trim().toLowerCase()));
 function navigate(next:string){setView(next);setQuery('');setMessage('')}
 return <><SiteHeader name={name}/><main id="main" className="network-layout"><aside className="network-sidebar"><section className="network-panel"><h2>Manage my network</h2><button className="network-menu" aria-pressed={view==='connections'} onClick={()=>navigate('connections')}><Icon name="network"/>Connections <span>{accepted.length}</span></button><button className="network-menu" aria-pressed={view==='following'} onClick={()=>navigate('following')}><Icon name="person"/>Following</button><button className="network-menu" aria-pressed={view==='followers'} onClick={()=>navigate('followers')}><Icon name="person"/>Followers</button><a className="network-menu" href="/companies"><Icon name="jobs"/>Pages</a>{['Groups','Events','Newsletters'].map(label=><div className="network-menu muted" key={label}><Icon name="network"/>{label}<small>Unavailable</small></div>)}</section><section className="sidebar-card"><h2>Grow your community</h2><p>Discover members who have chosen to share their profiles.</p><a className="button outline" href="/search">Find people</a></section><section className="sidebar-card"><h2>Appearance</h2><Appearance signedIn={!!name}/></section></aside><div className="network-content"><section className="network-panel network-tabs" aria-label="Network navigation"><button className="outline" aria-pressed={view==='invitations'} onClick={()=>navigate('invitations')}>Grow</button><button className="outline" disabled title="Career milestones are not available yet">Catch up · unavailable</button></section><p role="status">{message}</p>{expired?<section className="network-panel"><p>Sign in to manage your network.</p></section>:view==='followers'?<Followers/>:view==='following'?<Following/>:<section className="network-panel" aria-label="Network members"><div className="network-heading"><h1>{view==='connections'?'Connections':view==='sent'?'Sent invitations':'Invitations'} ({view==='connections'?accepted.length:view==='sent'?sent.length:incoming.length})</h1><button className="text-button" disabled={busy} onClick={()=>navigate(view==='sent'?'invitations':'sent')}>{view==='sent'?'Received invitations':'Manage sent'}</button></div><div className="network-search"><label>Search by name<input value={query} maxLength={120} onChange={event=>setQuery(event.target.value)}/></label><button className="outline" disabled={busy} onClick={()=>void load()}>Refresh network</button></div>{!busy&&!rows.length&&<p className="network-empty">{query?'No matching members.':view==='connections'?'No connections yet.':view==='sent'?'No sent invitations.':'No pending invitations. You’re all caught up.'}</p>}{rows.map(item=><article className="network-member" key={item.id}><div className="network-avatar" aria-hidden="true">{item.name.charAt(0)}</div><div className="network-member-copy"><h2>{item.name}</h2><p className="muted">{item.state==='ACCEPTED'?'Connected':item.incoming?'Wants to connect with you':'Your invitation is pending'}</p></div><div className="network-member-actions">{item.state==='ACCEPTED'?<button className="outline" disabled={busy} onClick={()=>void act(item,'REMOVE')}>Remove connection</button>:item.incoming?<><button className="text-button" disabled={busy} onClick={()=>void act(item,'IGNORE')}>Ignore</button><button className="outline" disabled={busy} onClick={()=>void act(item,'ACCEPT')}>Accept</button></>:<button className="outline" disabled={busy} onClick={()=>void act(item,'WITHDRAW')}>Withdraw</button>}</div></article>)}</section>}{!expired&&name&&<NetworkDiscovery/>}</div></main></>;
}
