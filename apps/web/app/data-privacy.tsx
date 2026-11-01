'use client';
import {useCallback,useEffect,useRef,useState} from 'react';
import {messagingFetch,MessagingSessionExpired,MessagingTimeout} from './messaging-api';
import SearchHistory from './search-history';
import type {components} from './api-schema';
type Export=components['schemas']['DataExport'];
export default function DataPrivacy({onExpired}:{onExpired:()=>void}){
 const [items,setItems]=useState<Export[]>([]),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true),[message,setMessage]=useState(''),[reauth,setReauth]=useState(false);
 const mounted=useRef(true),active=useRef<AbortController|null>(null),writing=useRef(false),requestId=useRef<string|null>(null);
 const fail=useCallback((error:unknown)=>{if(!mounted.current)return;if(error instanceof MessagingSessionExpired){setItems([]);onExpired()}else setMessage(error instanceof MessagingTimeout?'The request timed out. Refresh exports before retrying.':error instanceof TypeError?'Connection interrupted. Your request may have been saved; refresh before retrying.':(error as Error).message)},[onExpired]);
 const refresh=useCallback(async()=>{
  if(active.current||writing.current)return;
  const controller=new AbortController();active.current=controller;
  try{const response=await messagingFetch('/api/v1/me/exports',{cache:'no-store',signal:controller.signal});if(!response.ok)throw Error('Unable to load exports. Please retry.');const data:Export[]=await response.json();if(mounted.current)setItems(data)}
  catch(error){if(!controller.signal.aborted)fail(error)}finally{if(active.current===controller)active.current=null;if(mounted.current)setLoading(false)}
 },[fail]);
 useEffect(()=>{mounted.current=true;void refresh();const timer=setInterval(()=>void refresh(),5000);return()=>{mounted.current=false;clearInterval(timer);active.current?.abort()}},[refresh]);
 async function action(url:string,method:'PUT'|'DELETE'|'GET'){
  if(writing.current)return;writing.current=true;setBusy(true);setMessage('');active.current?.abort();active.current=null;
  try{
   const headers:Record<string,string>={};
   if(method!=='GET'){const token=await messagingFetch('/api/v1/csrf');if(!token.ok)throw Error('Unable to verify this action. Retry.');const csrf=await token.json();headers[csrf.header]=csrf.token}
   const response=await messagingFetch(url,{method,headers,cache:'no-store'});
   if(response.status===428){setReauth(true);throw Error('Verify your identity to request, download or remove an export.')}
   if(response.status===429)throw Error('Wait ten minutes between new exports. Existing downloads remain available.');
   if(response.status===409)throw Error('An export may already be preparing, or this download is not ready. Refresh exports.');
   if(response.status===410)throw Error('This export has expired. Request a new archive when the cooldown ends.');
   if(!response.ok)throw Error('Export unavailable. Refresh and retry.');
   setReauth(false);
   if(method==='GET'){
    const objectUrl=URL.createObjectURL(await response.blob());const link=document.createElement('a');link.href=objectUrl;link.download='meridian-business-data.json';document.body.append(link);link.click();link.remove();setTimeout(()=>URL.revokeObjectURL(objectUrl),1000);setMessage('Download started. Store your archive securely.');
   }else if(method==='PUT'){requestId.current=null;setMessage('Export requested. You can leave this page and return later.')}else setMessage('Export removed. Its download is no longer available.');
  }catch(error){fail(error)}finally{writing.current=false;if(mounted.current){setBusy(false);await refresh()}}
 }
 function request(){requestId.current??=crypto.randomUUID();void action('/api/v1/me/exports/'+requestId.current,'PUT')}
 const pending=items.some(item=>item.status==='QUEUED'||item.status==='PROCESSING');
 return <>
  <SearchHistory/>
  <section className="settings-card" aria-label="Download your data">
   <h2 className="settings-card-heading">Get a copy of your data</h2>
   <div className="settings-description"><p>Download your profile, photo, professional history, preferences, connections, authored posts and comments, sent messages, and your own job applications as a JSON archive.</p><p>Passwords, security credentials, session tokens, received message bodies and other applicants’ private data are excluded. Identity-provider records are managed separately.</p><p>Archives expire after 24 hours. One new request every ten minutes; up to 10 MiB and 10,000 records per section. Larger exports fail explicitly rather than omit records.</p><p><a href="http://localhost:8081/oauth2/authorization/keycloak?screen=reauth">Verify your identity</a> before requesting or downloading an archive. Verification is valid for five minutes.</p></div>
   <div className="settings-card-footer"><button disabled={busy||loading||pending||reauth} onClick={request}>{busy?'Please wait…':'Request data export'}</button><button className="text-button" disabled={busy} onClick={()=>void refresh()}>Refresh exports</button><p role="status">{message||(loading?'Loading export requests…':'')}</p></div>
  </section>
  <section className="settings-card" aria-label="Export requests"><h2 className="settings-card-heading">Your export requests</h2>
   {!loading&&!items.length&&<p className="settings-description">No exports requested yet.</p>}
   <ul className="session-list">{items.map(item=><li key={item.id}><div><strong>{{QUEUED:'Queued',PROCESSING:'Preparing your archive',READY:'Ready to download',FAILED:'Export failed',EXPIRED:'Expired or removed'}[item.status]}</strong><p>Requested {new Date(item.createdAt).toLocaleString()}</p>{item.expiresAt&&<p>Download deadline: {new Date(item.expiresAt).toLocaleString()}</p>}{item.failure&&<p>{item.failure==='TOO_LARGE'?'Your archive exceeds the supported size. No partial download was created.':'Generation failed after retries. You can request a new archive after the cooldown.'}</p>}</div><div className="export-actions">{item.status==='READY'&&<button disabled={busy} onClick={()=>void action(`/api/v1/me/exports/${item.id}/download`,'GET')}>Download export</button>}{item.status!=='EXPIRED'&&<button className="outline" disabled={busy} onClick={()=>{if(window.confirm('Remove this export request and its download? Your original account data is kept.'))void action(`/api/v1/me/exports/${item.id}`,'DELETE')}}>Remove export</button>}</div></li>)}</ul>
  </section>
  <section className="settings-card"><h2 className="settings-card-heading">Account lifecycle</h2><p className="settings-description">Account deactivation and deletion are not yet available. Removing an export deletes only the archive, not your account or original records.</p></section>
 </>;
}
