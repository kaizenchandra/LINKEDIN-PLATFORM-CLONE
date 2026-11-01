'use client';
import {useEffect,useState} from 'react';
import type {components} from './api-schema';
type Preferences=components['schemas']['Preferences'];
type Theme=Preferences['theme'];
function apply(theme:Theme){document.documentElement.dataset.theme=theme;try{localStorage.setItem('meridian-theme',theme)}catch{/* Storage may be unavailable; account persistence still works. */}}
export default function Appearance({signedIn}:{signedIn:boolean}){
 const [theme,setTheme]=useState<Theme>('system');
 const [preferences,setPreferences]=useState<Preferences|null>(null);
 const [busy,setBusy]=useState(false),[message,setMessage]=useState('');
 async function reload(){
  setBusy(true);try{const response=await fetch('/api/v1/me/preferences');if(!response.ok)throw Error('Unable to load account appearance. Retry when connected.');const current:Preferences=await response.json();setPreferences(current);setTheme(current.theme);apply(current.theme);setMessage('');}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}
 }
 useEffect(()=>{const initial=document.documentElement.dataset.theme;if(initial==='light'||initial==='dark'||initial==='system')setTheme(initial);if(signedIn)void reload();else setPreferences(null);},[signedIn]);
 async function change(next:Theme){
  const previous=theme;setTheme(next);apply(next);
  if(!signedIn)return;
  if(!preferences){setTheme(previous);apply(previous);return;}
  setBusy(true);try{
   const response=await fetch('/api/v1/csrf');if(!response.ok)throw Error('Session expired. Sign in again.');const csrf=await response.json();
   const result=await fetch('/api/v1/me/preferences',{method:'PUT',headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify({theme:next,version:preferences.version})});
   if(!result.ok)throw Error(result.status===409?'Appearance changed in another session. Reload preferences before retrying.':'Appearance could not be saved. Please retry.');
   setPreferences(await result.json());setMessage('Appearance saved to your account.');
  }catch(error){setTheme(previous);apply(previous);setMessage((error as Error).message)}finally{setBusy(false)}
 }
 return <div><label>Appearance<select disabled={busy||(signedIn&&!preferences)} value={theme} onChange={event=>void change(event.target.value as Theme)}><option>system</option><option>light</option><option>dark</option></select></label><p aria-live="polite">{message}</p>{signedIn&&<button disabled={busy} onClick={()=>void reload()}>Reload preferences</button>}</div>;
}
