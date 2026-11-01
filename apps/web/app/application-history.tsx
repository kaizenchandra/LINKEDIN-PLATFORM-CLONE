'use client';
import {useEffect,useRef,useState} from 'react';
import {applicationRequest,ApplicationSessionExpired} from './application-api';
import type {components} from './api-schema';
type History=components['schemas']['ApplicationHistory'];
const labels:Record<string,string>={SUBMITTED:'Submitted',REVIEWING:'Under review',ACCEPTED:'Accepted',REJECTED:'Rejected',WITHDRAWN:'Withdrawn'};
export default function ApplicationHistory({applicationId,version,onSessionExpired}:{applicationId:string;version:number;onSessionExpired:()=>void}){
 const [open,setOpen]=useState(false),[history,setHistory]=useState<History|null>(null),[message,setMessage]=useState(''),[retry,setRetry]=useState(0),[loading,setLoading]=useState(false);
 const expire=useRef(onSessionExpired);expire.current=onSessionExpired;
 useEffect(()=>{
  if(!open)return;const controller=new AbortController();let disposed=false;setLoading(true);setHistory(null);setMessage('Loading history…');
  void applicationRequest('/api/v1/applications/'+applicationId+'/history','GET',undefined,controller.signal).then(async response=>{const result:History=await response.json();if(!disposed){setHistory(result);setMessage(result.version>version?'History includes a newer change. The summary will refresh shortly.':'')}}).catch(cause=>{if(disposed)return;if(cause instanceof ApplicationSessionExpired)expire.current();else setMessage((cause as Error).message)}).finally(()=>{if(!disposed)setLoading(false)});
  return()=>{disposed=true;controller.abort()};
 },[applicationId,version,open,retry]);
 return <details className="application-history" onToggle={event=>setOpen(event.currentTarget.open)}><summary>View application history</summary>{open&&<><p aria-live="polite">{message}</p>{history&&<ol aria-label="Application history">{history.items.map(item=><li key={item.version}><strong>{labels[item.toStatus]}</strong><p>{item.actorRole==='APPLICANT'?'Applicant':item.actorRole==='RECRUITER'?'Recruiter':'Company owner'} · <time dateTime={item.createdAt}>{new Date(item.createdAt).toLocaleString()}</time></p>{item.fromStatus&&<p className="muted">Previous status: {labels[item.fromStatus]}</p>}</li>)}</ol>}<button className="outline" disabled={loading} onClick={()=>setRetry(value=>value+1)}>Refresh history</button></>}</details>;
}
