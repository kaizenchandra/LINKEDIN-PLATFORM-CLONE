'use client';
import {useEffect,useRef,useState} from 'react';
import type {components} from './api-schema';
type Connection=components['schemas']['Connection'];
export async function mutate(url:string,method:string,body:unknown){
 const response=await fetch('/api/v1/csrf');if(!response.ok)throw Error('Session expired. Sign in again.');const csrf=await response.json();
 const result=await fetch(url,{method,headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify(body)});
 if(!result.ok)throw Error(result.status===409?'Request unavailable, duplicate or changed. Reload your network.':'Unable to save. Please retry.');
}
export function Connect({recipientId}:{recipientId:string}){
 const requestId=useRef<string|null>(null);
 useEffect(()=>{requestId.current=null;setSent(false);setMessage('')},[recipientId]);
 const [busy,setBusy]=useState(false),[sent,setSent]=useState(false),[message,setMessage]=useState('');
 async function send(){setBusy(true);requestId.current??=crypto.randomUUID();try{await mutate('/api/v1/network','POST',{id:requestId.current,recipientId});setSent(true);setMessage('Connection request sent.');}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 return <div><button disabled={busy||sent} onClick={()=>void send()}>{sent?'Request sent':'Connect'}</button><p aria-live="polite">{message}</p><a href="/network">Manage network</a></div>;
}
export default function Network(){
 const [connections,setConnections]=useState<Connection[]>([]),[message,setMessage]=useState('Loading network…'),[busy,setBusy]=useState(false);
 async function load(){const response=await fetch('/api/v1/network');if(!response.ok)throw Error('Unable to load network. Please retry.');setConnections(await response.json());setMessage('');}
 useEffect(()=>{void load().catch(error=>setMessage(error.message))},[]);
 async function act(connection:Connection,action:string){setBusy(true);try{await mutate(`/api/v1/network/${connection.id}`,'PUT',{action,version:connection.version});try{await load();setMessage('Network updated.')}catch{setMessage('Change saved. Reload to see your latest network.')}}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 return <section id="network" aria-label="My network" className="experience"><h2>My network</h2><p><a href="/search">Find people</a></p><p>Connections: {connections.filter(item=>item.state==='ACCEPTED').length}</p><p role="status">{message}</p>{connections.length===0&&<p>No invitations or connections yet. Connect from a shared member profile.</p>}{connections.map(item=><article key={item.id}><h3>{item.name}</h3><p>{item.state==='ACCEPTED'?'Connected':item.incoming?'Invitation received':'Invitation sent'}</p>{item.state==='ACCEPTED'?<button disabled={busy} onClick={()=>{if(window.confirm(`Remove connection with ${item.name}?`))void act(item,'REMOVE')}}>Remove connection</button>:item.incoming?<><button disabled={busy} onClick={()=>void act(item,'ACCEPT')}>Accept invitation</button>{' '}<button disabled={busy} onClick={()=>void act(item,'IGNORE')}>Ignore invitation</button></>:<button disabled={busy} onClick={()=>void act(item,'WITHDRAW')}>Withdraw invitation</button>}</article>)}<button disabled={busy} onClick={()=>load().catch(error=>setMessage(error.message))}>Reload network</button></section>;
}
