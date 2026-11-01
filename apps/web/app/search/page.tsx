'use client';
import {FormEvent,useEffect,useRef,useState} from 'react';
import SiteHeader from '../site-header';
import {useSearchHistory,SearchHistoryPanel} from '../search-history';
import {messagingFetch} from '../messaging-api';
import type {components} from '../api-schema';
type Kind='people'|'companies'|'jobs'|'posts';
type Criteria={q:string;location:string;kind:Kind};
type Results={kind:'people';items:components['schemas']['PersonResult'][]}|{kind:'companies';items:components['schemas']['Organization'][]}|{kind:'jobs';items:components['schemas']['Job'][]}|{kind:'posts';items:components['schemas']['Post'][]};
const kinds:Kind[]=['people','companies','jobs','posts'];
const titles:Record<Kind,string>={people:'People',companies:'Companies',jobs:'Jobs',posts:'Posts'};
export default function Search(){
 const history=useSearchHistory();
 const [name,setName]=useState<string>(),[query,setQuery]=useState(''),[location,setLocation]=useState(''),[kind,setKind]=useState<Kind>('people'),[criteria,setCriteria]=useState<Criteria|null>(null);
 const [results,setResults]=useState<Results>({kind:'people',items:[]}),[cursor,setCursor]=useState<string|null>(null),[busy,setBusy]=useState(false),[message,setMessage]=useState('Search for people by name or headline.');
 const request=useRef(0),active=useRef<AbortController|null>(null);
 async function load(search:Criteria,after?:string){
  const current=++request.current;active.current?.abort();const controller=new AbortController();active.current=controller;
  setBusy(true);setMessage('Searching…');if(!after){setResults({kind:search.kind,items:[]});setCursor(null)}
  try{
   const params=new URLSearchParams({q:search.q});if(search.kind==='people'||search.kind==='jobs')params.set('location',search.location);
   if(after)params.set(search.kind==='posts'?'before':'after',after);
   const path={people:'/search/people',companies:'/organizations',jobs:'/jobs',posts:'/search/posts'}[search.kind];
   const response=await messagingFetch('/api/v1'+path+'?'+params,{cache:'no-store',signal:controller.signal});
   if(!response.ok)throw Error(response.status===400?'Enter 2–80 search characters.':'Search unavailable. Please retry.');
   const data=await response.json() as {items:Results['items'];nextCursor:string|null};if(current!==request.current)return;
   const key=(item:Results['items'][number])=>'memberId' in item?item.memberId:item.id;
   setResults(old=>({kind:search.kind,items:after&&old.kind===search.kind?[...old.items,...data.items.filter(item=>!old.items.some(previous=>key(previous)===key(item)))]:data.items} as Results));
   setCursor(data.nextCursor);setMessage(data.items.length?'Results updated.':search.kind==='people'?'No matching people. Try another name, headline or location.':'No matching '+search.kind+'. Try another search.');
  }catch(error){if(current===request.current)setMessage((error as Error).message)}finally{if(current===request.current)setBusy(false)}
 }
 useEffect(()=>{
  void fetch('/api/v1/me').then(async response=>{if(response.ok)setName((await response.json()).name)}).catch(()=>{});
  const restore=()=>{const params=new URLSearchParams(window.location.search),q=params.get('q')??'',place=params.get('location')??'',tab=params.get('type') as Kind;const next={q:q.trim(),location:place.trim(),kind:kinds.includes(tab)?tab:'people' as Kind};setQuery(q);setLocation(place);setKind(next.kind);setCriteria(q?next:null);
   if(q)void load(next);else{++request.current;active.current?.abort();setResults({kind:next.kind,items:[]});setCursor(null);setBusy(false);setMessage('Enter a search to get started.');}};
  restore();window.addEventListener('popstate',restore);return()=>{++request.current;active.current?.abort();window.removeEventListener('popstate',restore)};
 },[]);
 function navigate(next:Criteria){setKind(next.kind);setCriteria(next.q?next:null);const params=new URLSearchParams({q:next.q,type:next.kind});if(next.location&&(next.kind==='people'||next.kind==='jobs'))params.set('location',next.location);window.history.pushState(null,'','/search?'+params);if(next.q.length>=2)void load(next);else{++request.current;active.current?.abort();setResults({kind:next.kind,items:[]});setCursor(null);setBusy(false);setMessage('Enter at least two search characters.')}}
 function submit(event:FormEvent){event.preventDefault();const next={q:query.trim(),location:location.trim(),kind};navigate(next);history.remember(next)}
 return <><SiteHeader name={name}/><main id="main" className="profile-layout"><div className="profile-column"><section className="profile-card search-panel"><nav className="search-categories" aria-label="Search categories">{kinds.map(tab=><button key={tab} className={kind===tab?'':'outline'} aria-pressed={kind===tab} onClick={()=>navigate({q:query.trim(),location:location.trim(),kind:tab})}>{titles[tab]}</button>)}</nav><h1>Find {kind}</h1><p>{kind==='people'?'Discover professionals who have chosen to appear in people search.':kind==='posts'?'Search posts shared with members and your own private posts.':'Explore '+kind+' across the professional community.'}</p><form onSubmit={submit}><label>{kind==='people'?'Name or headline':'Search terms'}<input required minLength={2} maxLength={80} value={query} onChange={event=>setQuery(event.target.value)}/></label>{(kind==='people'||kind==='jobs')&&<label>Location (optional)<input maxLength={120} value={location} onChange={event=>setLocation(event.target.value)}/></label>}<button disabled={busy||query.trim().length<2}>{kind==='people'?'Search members':'Search '+kind}</button>{criteria&&<button type="button" className="outline" disabled={busy} onClick={()=>void load(criteria)}>Refresh results</button>}</form><p role="status">{message}</p></section>
 {results.kind==='people'&&results.items.map(person=><article className="profile-card person-result" key={person.memberId} aria-label={'Search result for '+person.name}><div className="mini-avatar" aria-hidden="true">{person.name.charAt(0)}</div><div><h2><a href={'/members/'+person.memberId}>{person.name}</a></h2><p>{person.headline||'Professional community member'}</p>{person.location&&<p className="muted">{person.location}</p>}<a className="button outline" href={'/members/'+person.memberId}>View profile</a></div></article>)}
 {results.kind==='companies'&&results.items.map(company=><article className="profile-card search-result" key={company.id}><h2><a href={'/companies/'+company.id}>{company.name}</a></h2><p>{company.industry} · {company.location}</p><p>{company.about}</p><a href={'/companies/'+company.id}>View company</a></article>)}
 {results.kind==='jobs'&&results.items.map(job=><article className="profile-card search-result" key={job.id}><h2><a href={'/jobs/'+job.id}>{job.title}</a></h2><p>{job.companyName} · {job.location} · {job.workplace.toLowerCase()}</p><a href={'/jobs/'+job.id}>View job</a></article>)}
 {results.kind==='posts'&&results.items.map(post=><article className="profile-card search-result" key={post.id}><h2>{post.authorName}</h2><p className="muted">{new Date(post.createdAt).toLocaleDateString()} · {post.audience==='ONLY_ME'?'Only you':'Members'}{post.edited?' · Edited':''}</p><p className="search-post-body">{post.text}</p><p>{post.reactionCount} reactions</p></article>)}
 {cursor&&criteria&&<button className="outline" disabled={busy} onClick={()=>void load(criteria,cursor)}>More {kind}</button>}</div><aside className="profile-sidebar" aria-label="Search preferences"><SearchHistoryPanel history={history} onReuse={next=>{setQuery(next.q);setLocation(next.location);navigate(next)}}/><section className="sidebar-card"><h2>Your discovery settings</h2><p className="muted">People search is opt-in. Blocked members cannot find one another or read each other’s posts.</p><a href="/#settings">Manage visibility</a></section></aside></main></>;
}
