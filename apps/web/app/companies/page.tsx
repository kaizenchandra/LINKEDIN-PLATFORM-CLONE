'use client';
import {FormEvent,useEffect,useRef,useState} from 'react';
import SiteHeader from '../site-header';
import Appearance from '../appearance';
import {companyRequest,CompanySessionExpired} from '../company-api';
import type {components} from '../api-schema';
type Organization=components['schemas']['Organization'];
export default function Companies(){
 const [name,setName]=useState<string>(),[query,setQuery]=useState(''),[criteria,setCriteria]=useState(''),[view,setView]=useState('ALL'),[items,setItems]=useState<Organization[]>([]),[cursor,setCursor]=useState<string|null>(null),[busy,setBusy]=useState(true),[message,setMessage]=useState('Loading companies…'),[expired,setExpired]=useState(false);
 const active=useRef<AbortController|null>(null),generation=useRef(0);
 async function load(q:string,mode:string,after?:string){
  active.current?.abort();const controller=new AbortController();active.current=controller;const version=++generation.current;setBusy(true);setMessage('Loading companies…');
  try{
   const [result,me]=await Promise.all([companyRequest('/api/v1/organizations?'+new URLSearchParams({q,view:mode,...(after?{after}:{})}), 'GET',undefined,controller.signal),companyRequest('/api/v1/me','GET',undefined,controller.signal)]);
   const data:components['schemas']['OrganizationPage']=await result.json();const account=await me.json();if(version!==generation.current)return;
   setName(account.name);setItems(data.items);setCursor(data.nextCursor);setMessage(data.items.length?'':'No companies match this view.');
  }catch(cause){if(version===generation.current&&!controller.signal.aborted){if(cause instanceof CompanySessionExpired){setExpired(true);setName(undefined);setItems([]);setCursor(null)}setMessage((cause as Error).message)}}finally{if(version===generation.current)setBusy(false)}
 }
 useEffect(()=>{void load('','ALL');return()=>{generation.current++;active.current?.abort()}},[]);
 function search(event:FormEvent){event.preventDefault();setCriteria(query.trim());void load(query.trim(),view)}
 return <><SiteHeader name={name}/><main id="main" className="profile-layout"><div className="profile-column"><section className="profile-card search-panel"><h1>Companies</h1><p>Explore member-created company pages and follow organizations that interest you.</p><form onSubmit={search}><label>Company name or industry<input maxLength={80} value={query} onChange={event=>setQuery(event.target.value)} disabled={busy||expired}/></label><button disabled={busy||expired}>Search companies</button></form><div className="hero-actions" aria-label="Company views">{[['ALL','All companies'],['MANAGED','Managed companies'],['FOLLOWING','Followed companies']].map(([key,label])=><button className="outline" key={key} aria-pressed={view===key} disabled={busy||expired} onClick={()=>{setView(key);void load(criteria,key)}}>{label}</button>)}</div><p role="status">{message}</p><button className="outline" disabled={busy||expired} onClick={()=>void load(criteria,view)}>Refresh companies</button></section>{items.map(company=><article key={company.id} className="profile-card person-result" aria-label={'Company '+company.name}><div className="mini-avatar" aria-hidden="true">{company.name.charAt(0)}</div><div><h2><a href={'/companies/'+company.id}>{company.name}</a></h2><p>{company.industry||'Industry not provided'}{company.location?' · '+company.location:''}</p><p className="muted">{company.followerCount} followers · Member-created page</p>{company.owned&&<p>You manage this page</p>}{company.following&&<p>Following</p>}<a className="button outline" href={'/companies/'+company.id}>View company</a></div></article>)}{cursor&&<button className="outline" disabled={busy||expired} onClick={()=>void load(criteria,view,cursor)}>More companies</button>}</div><aside className="profile-sidebar"><section className="sidebar-card"><h2>Build your company presence</h2><p>Company pages are visible to signed-in members. Creating a page does not verify ownership of a brand or employment.</p>{!expired&&<a className="button" href="/companies/new">Create company page</a>}</section><section className="sidebar-card"><h2>Appearance</h2><Appearance signedIn={!!name}/></section></aside></main></>;
}
