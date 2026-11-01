'use client';
import {useCallback,useEffect,useState} from 'react';
/** Shared transport and draft lifecycle; authorization remains on the backend. */
export function useProfileSection<T>(resource:string,label:string){
 const [entries,setEntries]=useState<T[]>([]);
 const [draft,setDraft]=useState<T|null>(null);
 const [message,setMessage]=useState(`Loading ${label.toLowerCase()}…`);
 const [busy,setBusy]=useState(false);
 const load=useCallback(async()=>{
  const response=await fetch(`/api/v1/me/${resource}`);
  if(!response.ok)throw Error(`Unable to load ${label.toLowerCase()}. Please retry.`);
  setEntries(await response.json());setMessage('');
 },[resource,label]);
 useEffect(()=>{load().catch(error=>setMessage(error.message));},[load]);
 useEffect(()=>{
  if(!draft)return;
  const warn=(event:BeforeUnloadEvent)=>event.preventDefault();
  window.addEventListener('beforeunload',warn);
  return()=>window.removeEventListener('beforeunload',warn);
 },[draft]);
 async function mutate(method:string,url:string,entry?:T){
  setBusy(true);
  try{
   const response=await fetch('/api/v1/csrf');
   if(!response.ok)throw Error('Session expired. Sign in again.');
   const csrf=await response.json();
   const result=await fetch(url,{method,headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:entry?JSON.stringify(entry):undefined});
   if(!result.ok)throw Error(result.status===409?'Entry already exists, changed or is unavailable. Cancel and reload before retrying.':'Unable to save. Check dates, links and required fields.');
   setDraft(null);
   try {await load();setMessage(`${label} ${method==='DELETE'?'removed':'saved'}.`);}
   catch {setMessage('Change saved, but the list could not refresh. Reload to retrieve current data.');}
  }catch(error){setMessage((error as Error).message)}finally{setBusy(false)}
 }
 return {entries,draft,setDraft,message,setMessage,busy,load,mutate};
}
