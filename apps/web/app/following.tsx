'use client';
import {useCallback,useEffect,useState} from 'react';
import {mutate} from './network';
import type {components} from './api-schema';
type Member=components['schemas']['FollowedMember'];
export function Follow({memberId}:{memberId:string}){
 const [following,setFollowing]=useState<boolean|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
 const load=useCallback(async()=>{setBusy(true);try{const response=await fetch('/api/v1/following/'+encodeURIComponent(memberId),{cache:'no-store'});if(!response.ok)throw Error('Follow status unavailable. Refresh to retry.');const state:components['schemas']['FollowState']=await response.json();setFollowing(state.following);setMessage('')}catch(error){setFollowing(null);setMessage((error as Error).message)}finally{setBusy(false)}},[memberId]);
 useEffect(()=>{void load()},[load]);
 async function change(){if(following===null)return;setBusy(true);try{await mutate('/api/v1/following/'+encodeURIComponent(memberId),'PUT',{following:!following});setFollowing(!following);setMessage(following?'Member unfollowed.':'Member followed.')}catch{setMessage('Follow change could not be confirmed. Refresh status before retrying.')}finally{setBusy(false)}}
 return <div><button className="outline" disabled={busy||following===null} onClick={()=>void change()}>{following?'Unfollow':'Follow'}</button><p aria-live="polite">{message}</p><button className="text-button" disabled={busy} onClick={()=>void load()}>Refresh follow status</button></div>;
}
export default function Following(){
 const [items,setItems]=useState<Member[]>([]),[total,setTotal]=useState(0),[cursor,setCursor]=useState<string|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState('Loading following…');
 async function load(before?:string){setBusy(true);try{const response=await fetch('/api/v1/following'+(before?'?before='+encodeURIComponent(before):''),{cache:'no-store'});if(!response.ok)throw Error('Unable to load following. Please retry.');const data:components['schemas']['FollowingPage']=await response.json();setItems(current=>before?[...current,...data.items.filter(item=>!current.some(old=>old.memberId===item.memberId))]:data.items);setCursor(data.nextCursor);setTotal(data.total);setMessage('')}catch(error){setMessage((error as Error).message)}finally{setBusy(false)}}
 useEffect(()=>{void load()},[]);
 async function remove(item:Member){setBusy(true);try{await mutate('/api/v1/following/'+encodeURIComponent(item.memberId),'PUT',{following:false});await load()}catch{setMessage('Unfollow could not be confirmed. Reload following before retrying.')}finally{setBusy(false)}}
 return <section className="experience" aria-label="Following"><h2>Following</h2><p>Following: {total}</p><p className="muted">Follow people independently of your connections. This list is private to you.</p><p role="status">{message}</p>{!items.length&&!message&&<p>You aren’t following anyone yet. Follow from a shared member profile.</p>}{items.map(item=><article key={item.memberId}><h3>{item.visible?<a href={'/members/'+item.memberId}>{item.name}</a>:item.name}</h3><button disabled={busy} aria-label={'Unfollow '+item.name} onClick={()=>void remove(item)}>Unfollow</button></article>)}{cursor&&<button disabled={busy} onClick={()=>void load(cursor)}>More followed members</button>}<button disabled={busy} onClick={()=>void load()}>Reload following</button></section>;
}
