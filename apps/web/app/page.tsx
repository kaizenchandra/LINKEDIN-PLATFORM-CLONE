'use client';
import {useEffect,useState,FormEvent,useRef} from 'react';
import SiteHeader,{Icon,ProfileSectionSearch} from './site-header';
import Welcome from './welcome';
import ProfilePhoto from './profile-photo';
import Experience from './experience';
import Education from './education';
import Certifications from './certifications';
import Projects from './projects';
import Skills from './skills';
import Languages from './languages';
import VisibilitySettings from './visibility';
import DiscoverySettings from './discovery';
import Network from './network';
import Following from './following';
import Blocks from './blocks';
import type {components} from './api-schema';
type Profile=components['schemas']['Profile'];
export default function Home(){
 const dialog=useRef<HTMLDialogElement>(null);
 const [draft,setDraft]=useState<Profile|null>(null);
 function edit(){if(profile){setDraft({...profile});dialog.current?.showModal();}}
 const [guest,setGuest]=useState(false);
 const [profile,setProfile]=useState<Profile|null>(null),[message,setMessage]=useState('Loading your workspace…'),[busy,setBusy]=useState(false);
 useEffect(()=>{fetch('/api/v1/me').then(async r=>{if(r.status===401)setGuest(true);if(!r.ok)throw Error(r.status===401?'Sign in to build your professional profile.':'Your workspace is unavailable. Please reload.');setProfile(await r.json());setMessage('');}).catch(e=>setMessage(new URLSearchParams(window.location.search).has('authentication')?'Sign-in could not be completed. Please try again.':e.message));},[]);
 async function logout(){
 setBusy(true);
 try {
 const response=await fetch('/api/v1/csrf');
 if(!response.ok)throw Error('Unable to sign out. Please retry.');
 const csrf=await response.json();
 const form=document.createElement('form');form.method='POST';form.action='/api/v1/logout';
 const input=document.createElement('input');input.type='hidden';input.name='_csrf';input.value=csrf.token;
 form.append(input);document.body.append(form);form.submit();
 }catch(error){setMessage((error as Error).message);setBusy(false)}
 }
 async function save(e:FormEvent){e.preventDefault();setBusy(true);try{const c=await fetch('/api/v1/csrf');if(!c.ok)throw Error('Session expired. Sign in again.');const csrf=await c.json();const r=await fetch('/api/v1/me',{method:'PUT',headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify(draft)});if(!r.ok)throw Error(r.status===409?'Your profile changed in another session. Reload before saving.':'Unable to save. Your edits are still here.');setProfile(await r.json());setMessage('Profile saved.');dialog.current?.close();setDraft(null);}catch(e){setMessage((e as Error).message)}finally{setBusy(false)}}
 function closeEditor(){if(draft&&JSON.stringify(draft)!==JSON.stringify(profile)&&!window.confirm('Discard unsaved profile changes?'))return;dialog.current?.close();setDraft(null);}
 const initials=profile?.name.split(' ').map(part=>part[0]).slice(0,2).join('')??'m';
 if(guest)return <Welcome message={message.includes('could not')?message:undefined}/>;
 return <><SiteHeader name={profile?.name} busy={busy} onLogout={()=>void logout()}/><main id="main" className="profile-layout"><div className="profile-column"><section className="profile-hero" aria-label="Profile introduction"><div className="cover"><span className="cover-wordmark">meridian <span>Make your next connection.</span></span></div><div className="hero-body">{profile?<ProfilePhoto name={profile.name}/>:<div className="avatar" aria-hidden="true">{initials}</div>}{profile&&<button className="icon-button edit-intro" onClick={edit} aria-label="Edit introduction"><Icon name="edit"/></button>}<div className="profile-identity"><h1>{profile?.name??'Welcome to Meridian'}</h1><p className="headline">{profile?.headline||'Your experience. Your perspective. Your next chapter.'}</p><p className="muted">{profile?.location||'Add your location to complete your introduction.'}</p></div><p role="status" className="save-status">{message}</p>{profile?<><div className="hero-actions"><button onClick={edit}>Edit profile</button><a className="button outline" href="#experience">Add profile section</a><a className="button neutral" href="#network">My network</a></div><div className="profile-prompt"><strong>Make your profile your own</strong><p>Share the experience and skills that tell your professional story.</p><a href="#settings">Manage profile visibility →</a></div></>:<div className="profile-prompt"><strong>Your professional community starts here</strong><p>Sign in or create an account to build your profile and connect with people.</p></div>}</div></section>
 {profile&&<><section className="profile-card" id="about"><div className="section-title"><h2>About</h2><button className="icon-button" aria-label="Edit about" onClick={edit}><Icon name="edit"/></button></div><p className="preserve-lines">{profile.about||'Introduce yourself, the work you care about, and what you would like to do next.'}</p></section><div id="experience"><Experience/></div><div id="education"><Education/></div><div id="certifications"><Certifications/></div><div id="projects"><Projects/></div><div id="skills"><Skills/></div><div id="languages"><Languages/></div><Network/><Following/><div id="settings" className="settings-heading"><Icon name="shield"/><h2>Settings &amp; privacy</h2></div><VisibilitySettings/><DiscoverySettings/><Blocks/></>}
 </div><aside className="profile-sidebar" aria-label="Profile tools"><section className="sidebar-card"><h2>Your profile</h2><ProfileSectionSearch/><p className="muted">Keep your professional story up to date.</p><a className="sidebar-row" href="#about">About <span>↗</span></a><a className="sidebar-row" href="#experience">Experience <span>↗</span></a><a className="sidebar-row" href="#skills">Skills <span>↗</span></a>{profile&&<a className="sidebar-row" href="#settings">Profile visibility <Icon name="edit"/></a>}</section><section className="sidebar-card community-card"><div className="community-symbol" aria-hidden="true">m.</div><h2>A little more you.<br/>A world of possibility.</h2><p>Build a profile that opens the door to your next conversation.</p><a className="button outline" href={profile?'#network':'http://localhost:8081/oauth2/authorization/keycloak'}>{profile?'Manage my network':'Join Meridian'}</a></section><footer className="sidebar-footer"><a href="#settings">Privacy &amp; settings</a><span>Meridian · Professional community</span></footer></aside></main>
 <dialog aria-labelledby="introduction-title" ref={dialog} className="profile-dialog" onCancel={event=>{event.preventDefault();closeEditor();}}><div className="dialog-header"><h2 id="introduction-title">Edit introduction</h2><button className="icon-button" aria-label="Close introduction editor" onClick={closeEditor}>×</button></div>{draft&&<form onSubmit={save}><p className="muted">Help people get to know you and your work.</p>{(['name','headline','location','about'] as const).map(key=><label key={key}>{key}<textarea required={key==='name'} maxLength={{name:120,headline:220,location:120,about:2600}[key]} rows={key==='about'?5:1} value={draft[key]} onChange={e=>setDraft({...draft,[key]:e.target.value})}/></label>)}<p aria-live="polite">{message}</p><div className="dialog-actions"><button type="button" className="button neutral" onClick={closeEditor}>Cancel</button><button disabled={busy}>{busy?'Saving…':'Save profile'}</button></div></form>}</dialog></>;
}
