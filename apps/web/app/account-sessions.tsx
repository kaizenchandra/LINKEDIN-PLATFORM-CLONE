'use client';
import {useCallback,useEffect,useRef,useState} from 'react';
import {messagingFetch,MessagingSessionExpired} from './messaging-api';
import type {components} from './api-schema';
type AccountSession=components['schemas']['AccountSession'];
export default function AccountSessions({onExpired}:{onExpired:()=>void}){
 const [sessions,setSessions]=useState<AccountSession[]>([]),[busy,setBusy]=useState(false),[message,setMessage]=useState('Loading sessions…');
 const generation=useRef(0),mounted=useRef(true);
 const refresh=useCallback(async(silent=false)=>{const ticket=++generation.current;try{
  const response=await messagingFetch('/api/v1/me/sessions',{cache:'no-store'});if(!response.ok)throw Error('Unable to load sessions. Please retry.');const rows:AccountSession[]=await response.json();
  if(mounted.current&&ticket===generation.current){setSessions(rows);if(!silent)setMessage('');}
 }catch(error){if(!mounted.current||ticket!==generation.current)return;if(error instanceof MessagingSessionExpired){setSessions([]);onExpired();}else setMessage((error as Error).message)}},[onExpired]);
 useEffect(()=>{mounted.current=true;void refresh();const focus=()=>void refresh(true);window.addEventListener('focus',focus);const interval=setInterval(focus,15000);return()=>{mounted.current=false;generation.current++;clearInterval(interval);window.removeEventListener('focus',focus)}},[refresh]);
 async function revoke(item?:AccountSession){
  if(!window.confirm(item?`Sign out ${item.current?'this':'the selected'} Meridian session?`:'Sign out of Meridian on all devices, including this one?'))return;
  generation.current++;setBusy(true);setMessage('Signing out…');try{
   const token=await messagingFetch('/api/v1/csrf');if(!token.ok)throw Error('Unable to verify this action. Retry.');const csrf=await token.json();
   const response=await messagingFetch('/api/v1/me/sessions'+(item?`/${item.id}`:''),{method:'DELETE',headers:{[csrf.header]:csrf.token}});
   if(!response.ok)throw Error('Sign-out could not be confirmed. Refresh sessions before retrying.');
   if(!item||item.current){setSessions([]);onExpired();return;}
   await refresh(true);setMessage('Session signed out.');
  }catch(error){if(error instanceof MessagingSessionExpired){setSessions([]);onExpired();}else setMessage((error as Error).message)}finally{setBusy(false)}
 }
 const date=(value:string)=>new Date(value).toLocaleString();
 return <section className="settings-card" aria-label="Where you are signed in"><div className="settings-card-heading"><h2>Where you are signed in</h2><button className="text-button" disabled={busy} onClick={()=>void refresh()}>Refresh sessions</button></div><p className="settings-description">These are your active Meridian sessions. Signing out stops further access from that session. New sign-ins require authentication again. Session expiry is the earlier of the idle timeout and the absolute sign-in deadline. Activity cannot extend the absolute deadline.</p><p className="settings-description">End a Keycloak sign-in to also revoke its linked Meridian sessions in <a href="http://localhost:28080/realms/meridian/account/account-security/device-activity">identity provider sessions</a>.</p><p className="settings-description" role="status">{message}</p><ul className="session-list">{sessions.map((session,index)=><li key={session.id}><div><strong>{session.current?'This session':`Other session ${index+1}`}</strong><p>Signed in <time dateTime={session.createdAt}>{date(session.createdAt)}</time></p><p>Last active <time dateTime={session.lastActiveAt}>{date(session.lastActiveAt)}</time></p><p>Session expiry <time dateTime={session.expiresAt}>{date(session.expiresAt)}</time></p></div><button className="outline" disabled={busy} aria-label={session.current?'Sign out this session':`Sign out other session ${index+1}`} onClick={()=>void revoke(session)}>Sign out</button></li>)}</ul>{!sessions.length&&!message&&<p className="settings-description">No active sessions found. Refresh to check again.</p>}<div className="settings-card-footer"><button className="outline" disabled={busy||!sessions.length} onClick={()=>void revoke()}>Sign out of Meridian on all devices</button><p className="muted">Includes this session. Your profile and business data are kept.</p></div></section>;
}
