'use client';
import {use,useCallback,useEffect,useState} from 'react';
import {ProfileAvatar} from '../../profile-photo';
import SiteHeader from '../../site-header';
import {Connect} from '../../network';
import {Follow} from '../../following';
import PostActivity from '../../post-activity';
import StartMessage from '../../start-message';
import {NetworkDiscovery} from '../../network-discovery';
import {BlockMember} from '../../blocks';
import type {components} from '../../api-schema';
type Summary=components['schemas']['MemberSummary'];
export default function Member({params}:{params:Promise<{id:string}>}){
 const {id}=use(params);const [profile,setProfile]=useState<Summary|null>(null),[message,setMessage]=useState('Loading profile…'),[viewer,setViewer]=useState<string|undefined>();
 const load=useCallback(async()=>{setProfile(null);setMessage('Loading profile…');try{const response=await fetch(`/api/v1/profiles/${encodeURIComponent(id)}`,{cache:'no-store'});if(response.status===401){window.location.replace(`/api/v1/profile-login/${encodeURIComponent(id)}`);return;}if(!response.ok)throw Error(response.status===401?'Sign in to view member profiles.':response.status===404?'Profile unavailable or private.':'Unable to load profile. Please retry.');const summary=await response.json();const session=await fetch('/api/v1/me',{cache:'no-store'});setViewer(session.ok?(await session.json()).name:undefined);setProfile(summary);setMessage('');}catch(error){setMessage((error as Error).message)}},[id]);
 useEffect(()=>{void load();const refresh=()=>void load();window.addEventListener('focus',refresh);return()=>window.removeEventListener('focus',refresh)},[load]);
 return <><SiteHeader name={viewer}/><main id="main" className="profile-layout member-profile"><div className="profile-column"><section className="profile-hero"><div className="cover" aria-hidden="true"/><div className="hero-body">{profile&&<ProfileAvatar src={`/api/v1/profiles/${encodeURIComponent(id)}/photo`} name={profile.name}/>}<p className="member-load-status" role="status">{message}</p>{profile&&<><div className="member-identity"><h1>{profile.name}</h1>{profile.headline&&<p className="headline">{profile.headline}</p>}{profile.location&&<p className="muted">{profile.location}</p>}</div><div className="member-actions" aria-label="Member actions">{viewer?<><Connect recipientId={id}/><StartMessage memberId={id}/><Follow memberId={id}/></>:<a className="button outline" href={`/api/v1/profile-login/${encodeURIComponent(id)}`}>Sign in to connect</a>}</div></>}</div></section>{profile&&<section className="profile-card"><h2>About</h2><p className="preserve-lines">{profile.about||'This member has not added an introduction yet.'}</p></section>}{profile&&viewer&&<><PostActivity memberId={id}/><NetworkDiscovery memberId={id}/></>}</div><aside className="profile-sidebar"><section className="sidebar-card member-tools"><h2>Member profile</h2><p className="muted">Build your network, one conversation at a time.</p>{profile&&viewer&&<details className="member-safety"><summary>Safety options</summary><p className="muted">Control who can interact with you.</p><BlockMember memberId={id}/></details>}<button className="outline" onClick={()=>void load()}>Refresh profile</button><a className="button neutral" href="/">{viewer?'Back to my profile':'Back to home'}</a></section></aside></main></>;
}
