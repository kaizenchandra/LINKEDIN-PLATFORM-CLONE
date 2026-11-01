'use client';
import {useEffect,useState} from 'react';
import type {components} from './api-schema';
type Visibility=components['schemas']['Visibility'];
export default function VisibilitySettings(){
 const [value,setValue]=useState<Visibility|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState('Loading visibility…');
 async function load(){try{const response=await fetch('/api/v1/me/visibility');if(!response.ok)throw Error('Unable to load visibility. Please retry.');setValue(await response.json());setMessage('');}catch(error){setMessage((error as Error).message)}}
 useEffect(()=>{void load()},[]);
 async function change(memberVisible:boolean,publicVisible=value?.publicVisible??false){if(!value)return;setBusy(true);try{
 const response=await fetch('/api/v1/csrf');if(!response.ok)throw Error('Session expired. Sign in again.');const csrf=await response.json();
 const result=await fetch('/api/v1/me/visibility',{method:'PUT',headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify({memberVisible,publicVisible,version:value.version})});
 if(!result.ok)throw Error(result.status===409?'Visibility changed elsewhere. Reload before retrying.':'Visibility was not changed. Please retry.');
 setValue(await result.json());setMessage('Visibility saved.');
 }catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 return <section aria-label="Profile visibility" className="experience"><h2>Profile visibility</h2><p>Share your name, headline, location and about section with signed-in members who have your profile link. Public sharing also allows visitors without an account to view these fields. Other profile sections remain private.</p><label><input type="checkbox" checked={value?.memberVisible??false} disabled={busy||!value} onChange={event=>void change(event.target.checked)}/> Visible to signed-in members</label><label><input type="checkbox" checked={value?.publicVisible??false} disabled={busy||!value} onChange={event=>void change(value?.memberVisible??false,event.target.checked)}/> Public profile — visible without signing in</label><p aria-live="polite">{message}</p>{value&&<a href={`/members/${value.publicId}`}>View member profile</a>}{' '}<button disabled={busy} onClick={()=>void load()}>Reload visibility</button></section>;
}
