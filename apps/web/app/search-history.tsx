'use client';
import {useCallback,useEffect,useRef,useState} from 'react';
import {messagingFetch,MessagingSessionExpired} from './messaging-api';
import type {components} from './api-schema';
type State=components['schemas']['SearchHistoryState'];
export type SearchCriteria={q:string;location:string;kind:components['schemas']['SearchHistoryEntry']['kind']};
export function useSearchHistory(){
 const [state,setState]=useState<State|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState(''),[expired,setExpired]=useState(false);
 const mounted=useRef(true),serial=useRef(0),writing=useRef(false),current=useRef<State|null>(null);
 const accept=useCallback((next:State)=>{current.current=next;if(mounted.current){setState(next);setExpired(false)}},[]);
 const fail=useCallback((error:unknown)=>{if(!mounted.current)return;if(error instanceof MessagingSessionExpired){current.current=null;setState(null);setExpired(true);setMessage('Sign in to manage search history.')}else setMessage((error as Error).message)},[]);
 const refresh=useCallback(async()=>{
  if(writing.current)return;const turn=++serial.current;
  try{const response=await messagingFetch('/api/v1/me/search-history',{cache:'no-store'});if(!response.ok)throw Error('Search history unavailable. Retry to check its saved state.');const next:State=await response.json();if(mounted.current&&turn===serial.current)accept(next)}catch(error){if(turn===serial.current)fail(error)}
 },[accept,fail]);
 useEffect(()=>{mounted.current=true;void refresh();const focus=()=>void refresh();window.addEventListener('focus',focus);const interval=setInterval(focus,15000);return()=>{mounted.current=false;++serial.current;window.removeEventListener('focus',focus);clearInterval(interval)}},[refresh]);
 async function change(path:string,method:'PUT'|'DELETE',body?:unknown){
  if(writing.current)return;writing.current=true;++serial.current;setBusy(true);setMessage('Updating search history…');
  try{
   const token=await messagingFetch('/api/v1/csrf');if(!token.ok)throw Error('Unable to verify this action. Please retry.');const csrf=await token.json();
   const response=await messagingFetch('/api/v1/me/search-history'+path,{method,headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:body===undefined?undefined:JSON.stringify(body)});
   if(response.status===409)throw Error('Search history changed in another session. Refresh before retrying.');
   if(!response.ok)throw Error('Search history could not be confirmed. Refresh before retrying.');
   const next:State=await response.json();if(mounted.current){accept(next);setMessage(method==='DELETE'?'Search history removed.':path==='/entries'?'Search saved privately.':next.enabled?'Search history enabled.':'Search history disabled and cleared.')}
  }catch(error){fail(error)}finally{writing.current=false;if(mounted.current){setBusy(false);await refresh()}}
 }
 function remember(criteria:SearchCriteria){const saved=current.current;if(!saved?.enabled||criteria.q.trim().length<2)return;void change('/entries','PUT',{kind:criteria.kind,query:criteria.q,location:criteria.location,version:saved.version})}
 return {state,busy,message,expired,refresh,remember,change};
}
type History=ReturnType<typeof useSearchHistory>;
export function SearchHistoryPanel({history,onReuse}:{history:History;onReuse?:(criteria:SearchCriteria)=>void}){
 const {state,busy,message,expired,refresh,change}=history;
 return <section className="settings-card" aria-label="Search history"><h2 className="settings-card-heading">Search history</h2><div className="settings-description"><p>Save up to 20 recent searches privately for 30 days. Saving is off until you enable it. Searches submitted on the search page are saved; opening links and refreshing pages are not. Disabling clears your saved searches.</p>
 {expired?<a href="http://localhost:8081/oauth2/authorization/keycloak">Sign in to manage search history</a>:<label className="checkbox-label"><input type="checkbox" checked={state?.enabled??false} disabled={busy||!state} onChange={event=>{if(!state)return;if(!event.target.checked&&!window.confirm('Disable search history and remove all saved searches?'))return;void change('/preference','PUT',{enabled:event.target.checked,version:state.version})}}/>Save search history</label>}
 <p role="status">{message||(!state&&!expired?'Loading search history…':'')}</p>
 <button className="text-button" disabled={busy} onClick={()=>void refresh()}>Refresh history</button>
 {state&&<button className="text-button" disabled={busy||!state.items.length} onClick={()=>{if(window.confirm('Clear all saved searches?'))void change('?version='+state.version,'DELETE')}}>Clear search history</button>}
 {state?.enabled&&!state.items.length&&<p>No saved searches.</p>}
 <ul className="session-list search-history-list">{state?.items.map(entry=><li key={entry.id}><div>{onReuse?<button className="text-button" disabled={busy} aria-label={'Search again: '+entry.query+' ('+entry.kind+')'} onClick={()=>onReuse({q:entry.query,location:entry.location,kind:entry.kind})}>{entry.query}</button>:<a href={'/search?'+new URLSearchParams({q:entry.query,type:entry.kind,...(entry.location?{location:entry.location}:{})})}>{entry.query}</a>}<p className="muted">{entry.kind}{entry.location?' · '+entry.location:''} · <time dateTime={entry.searchedAt}>{new Date(entry.searchedAt).toLocaleDateString()}</time></p></div><button className="text-button" disabled={busy} aria-label={'Remove search: '+entry.query+' ('+entry.kind+')'} onClick={()=>void change('/entries/'+entry.id+'?version='+state.version,'DELETE')}>Remove</button></li>)}</ul>
 <p className="muted">Clearing history does not remove browser history or existing data exports or downloads.</p></div></section>;
}
export default function SearchHistory(){const history=useSearchHistory();return <SearchHistoryPanel history={history}/>}
