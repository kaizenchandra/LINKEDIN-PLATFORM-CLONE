'use client';
import {useEffect,useState} from 'react';
import type {components} from './api-schema';
type Discovery=components['schemas']['Discovery'];
export default function DiscoverySettings(){
 const [value,setValue]=useState<Discovery|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState('Loading discovery preference…');
 async function load(){setBusy(true);try{const result=await fetch('/api/v1/me/discovery',{cache:'no-store'});if(!result.ok)throw Error('Unable to load discovery preference. Retry.');setValue(await result.json());setMessage('')}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 useEffect(()=>{void load()},[]);
 async function change(discoverable:boolean){if(!value)return;setBusy(true);setMessage('Saving discovery preference…');try{const token=await fetch('/api/v1/csrf');if(!token.ok)throw Error('Session expired. Sign in again.');const csrf=await token.json();const result=await fetch('/api/v1/me/discovery',{method:'PUT',headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify({discoverable,version:value.version})});if(!result.ok)throw Error(result.status===409?'Discovery preference changed elsewhere. Reload before retrying.':'Discovery preference was not confirmed. Reload before retrying.');setValue(await result.json());setMessage('Discovery preference saved.')}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 return <section className="experience" aria-label="People search visibility"><h2>People search visibility</h2><p>Allow signed-in members to find your name, headline and location in people search. Your profile must also be visible to signed-in members. Turning either setting off hides you from new search results.</p><label><input type="checkbox" checked={value?.discoverable??false} disabled={busy||!value} onChange={event=>void change(event.target.checked)}/> Appear in people search</label><p role="status">{message}</p><button disabled={busy} onClick={()=>void load()}>Reload discovery preference</button></section>;
}
