'use client';
import {useCallback,useEffect,useRef,useState} from 'react';
import {messagingFetch} from './messaging-api';
import type {components} from './api-schema';
type Post=components['schemas']['Post'];
export default function PostActivity({memberId}:{memberId:string}){
 const [items,setItems]=useState<Post[]>([]),[cursor,setCursor]=useState<string|null>(null),[busy,setBusy]=useState(true),[message,setMessage]=useState('Loading activity…');
 const sequence=useRef(0),active=useRef<AbortController|null>(null);
 const load=useCallback(async(before?:string)=>{const serial=++sequence.current;active.current?.abort();const c=new AbortController();active.current=c;setBusy(true);if(!before){setItems([]);setCursor(null)}try{const r=await messagingFetch('/api/v1/activity/'+encodeURIComponent(memberId)+(before?'?before='+encodeURIComponent(before):''),{cache:'no-store',signal:c.signal});if(!r.ok)throw Error('Activity unavailable. Refresh to retry.');const data:components['schemas']['FeedPage']=await r.json();if(serial!==sequence.current)return;setItems(old=>before?[...old,...data.items.filter(p=>!old.some(x=>x.id===p.id))]:data.items);setCursor(data.nextCursor);setMessage(data.items.length?'':'No posts shared with you.')}catch(e){if(serial===sequence.current){setItems([]);setCursor(null);setMessage((e as Error).message)}}finally{if(serial===sequence.current)setBusy(false)}},[memberId]);
 useEffect(()=>{void load();return()=>{++sequence.current;active.current?.abort()}},[load]);
 return <section className="profile-card" aria-label="Post activity"><h2>Activity</h2><p className="muted">Recent posts shared with you.</p><p role="status">{message}</p>{items.map(post=><article key={post.id} className="activity-post"><time dateTime={post.createdAt}>{new Date(post.createdAt).toLocaleDateString()}</time><span> · {post.audience==='ONLY_ME'?'Only you':'Members'}{post.edited?' · Edited':''}</span><p className="post-body">{post.text}</p><p>{post.reactionCount} reactions</p></article>)}{cursor&&<button className="outline" disabled={busy} onClick={()=>void load(cursor)}>More activity</button>}<button className="text-button" disabled={busy} onClick={()=>void load()}>Refresh activity</button></section>;
}
