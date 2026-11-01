'use client';
import {useEffect,useState} from 'react';
import {mutate} from './network';
import type {components} from './api-schema';
export function BlockMember({memberId}:{memberId:string}){
 const [busy,setBusy]=useState(false),[message,setMessage]=useState('');
 async function block(){if(!window.confirm('Block this member? Active invitations, connections and follows in both directions will be removed.'))return;setBusy(true);try{await mutate('/api/v1/blocks','POST',{memberId});window.location.assign('/#blocks');}catch(error){setMessage((error as Error).message);setBusy(false)}}
 return <div><button disabled={busy} onClick={()=>void block()}>Block member</button><p aria-live="polite">{message}</p></div>;
}
export default function Blocks(){
 const [items,setItems]=useState<components['schemas']['Block'][]>([]),[message,setMessage]=useState('Loading blocked members…'),[busy,setBusy]=useState(false);
 async function load(){const response=await fetch('/api/v1/blocks');if(!response.ok)throw Error('Unable to load blocked members.');setItems(await response.json());setMessage('');}
 useEffect(()=>{void load().catch(error=>setMessage(error.message))},[]);
 async function unblock(id:string){if(!window.confirm('Unblock this member? Previous connections and follows will not be restored.'))return;setBusy(true);try{await mutate(`/api/v1/blocks/${id}`,'DELETE',{});await load();setMessage('Member unblocked.')}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 return <section id="blocks" className="experience" aria-label="Blocked members"><h2>Blocked members</h2><p role="status">{message}</p>{items.length===0&&<p>You have not blocked any members.</p>}{items.map(item=><article key={item.memberId}><h3>{item.name}</h3><button disabled={busy} onClick={()=>void unblock(item.memberId)}>Unblock {item.name}</button></article>)}<button disabled={busy} onClick={()=>load().catch(error=>setMessage(error.message))}>Reload blocked members</button></section>;
}
