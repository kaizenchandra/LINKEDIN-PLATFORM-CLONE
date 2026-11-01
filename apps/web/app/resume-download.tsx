'use client';
import {useEffect,useRef,useState} from 'react';
import {applicationRequest,ApplicationSessionExpired} from './application-api';
export default function ResumeDownload({applicationId,onExpired}:{applicationId:string;onExpired:()=>void}){
 const [busy,setBusy]=useState(false),[error,setError]=useState('');const active=useRef<AbortController|null>(null);
 useEffect(()=>()=>active.current?.abort(),[]);
 async function download(){
  const controller=new AbortController();active.current=controller;setBusy(true);setError('');
  try{
   const response=await applicationRequest('/api/v1/applications/'+applicationId+'/resume','GET',undefined,controller.signal);
   const blob=await response.blob();if(controller.signal.aborted)return;
   const url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download='resume.txt';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
  }catch(cause){if(!controller.signal.aborted){if(cause instanceof ApplicationSessionExpired)onExpired();else setError(cause instanceof Error?cause.message:'Resume unavailable.')}}finally{if(!controller.signal.aborted)setBusy(false)}
 }
 return <div><button className="outline" disabled={busy} onClick={()=>void download()}>{busy?'Downloading resume…':'Download resume (.txt)'}</button>{error&&<p role="status">{error}</p>}</div>;
}
