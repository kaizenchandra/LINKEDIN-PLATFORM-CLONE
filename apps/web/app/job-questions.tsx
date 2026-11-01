'use client';
import {useEffect,useRef,useState} from 'react';
import {jobRequest,JobSessionExpired} from './job-api';
import type {components} from './api-schema';
type Form=components['schemas']['JobQuestionForm'];
export default function JobQuestions({jobId,onExpired}:{jobId:string;onExpired:()=>void}){
 const [form,setForm]=useState<Form|null>(null),[draft,setDraft]=useState<Form|null>(null),[busy,setBusy]=useState(true),[message,setMessage]=useState('Loading questions…'),[dirty,setDirty]=useState(false);
 const [removed,setRemoved]=useState<{question:Form['questions'][number];index:number}|null>(null);
 const active=useRef<AbortController|null>(null),changed=useRef(false);changed.current=dirty;
 function fail(error:unknown){if(error instanceof JobSessionExpired){setForm(null);setDraft(null);onExpired()}setMessage((error as Error).message)}
 async function load(){
  if(changed.current&&!window.confirm('Discard unsaved question changes?'))return;
  active.current?.abort();const controller=new AbortController();active.current=controller;setBusy(true);
  try{const value:Form=await (await jobRequest('/api/v1/jobs/'+jobId+'/questions','GET',undefined,controller.signal)).json();if(!controller.signal.aborted){setForm(value);setDraft(null);setRemoved(null);setDirty(false);setMessage('')}}catch(error){if(!controller.signal.aborted)fail(error)}finally{if(!controller.signal.aborted)setBusy(false)}
 }
 useEffect(()=>{void load();return()=>active.current?.abort()},[jobId]);
 useEffect(()=>{const warn=(e:BeforeUnloadEvent)=>{if(changed.current)e.preventDefault()};window.addEventListener('beforeunload',warn);return()=>window.removeEventListener('beforeunload',warn)},[]);
 function move(index:number,direction:number){
  if(!draft||busy)return;const target=index+direction;if(target<0||target>=draft.questions.length)return;
  const questions=[...draft.questions];[questions[index],questions[target]]=[questions[target],questions[index]];
  setDraft({...draft,questions});setDirty(true);setMessage('Question moved to position '+(target+1)+'. Save to apply the new order.');
 }
 function remove(index:number){
  if(!draft||busy)return;setRemoved({question:draft.questions[index],index});
  setDraft({...draft,questions:draft.questions.filter((_,position)=>position!==index)});setDirty(true);
  setMessage('Question removed from the draft. Undo is available until you save or remove another question.');
 }
 function undoRemove(){
  if(!draft||!removed||busy||draft.questions.length>=5)return;
  const questions=[...draft.questions];questions.splice(Math.min(removed.index,questions.length),0,removed.question);
  setDraft({...draft,questions});setRemoved(null);setDirty(true);setMessage('Removed question restored. Save to apply your changes.');
 }
 async function save(event:React.FormEvent){event.preventDefault();if(!draft||busy)return;setBusy(true);
  try{const value:Form=await (await jobRequest('/api/v1/jobs/'+jobId+'/questions','PUT',draft)).json();setForm(value);setDraft(null);setRemoved(null);setDirty(false);setMessage('Application questions saved. Existing submissions keep their original questions.')}catch(error){fail(error)}finally{setBusy(false)}
 }
 return <section className="profile-card" aria-label="Application question management"><h2>Application questions</h2><p>Add up to five job-related questions. Required questions must be answered before submission. Answers are shared with your authorized recruiting team.</p><p role="status">{message}</p>{draft?<form className="company-form" onSubmit={save}>{draft.questions.map((question,index)=><fieldset key={question.id}><legend>Question {index+1}</legend><label htmlFor={'question-prompt-'+question.id}>Question {index+1} prompt</label><textarea id={'question-prompt-'+question.id} required maxLength={300} rows={3} disabled={busy} value={question.prompt} onChange={event=>{setDraft({...draft,questions:draft.questions.map(q=>q.id===question.id?{...q,prompt:event.target.value}:q)});setDirty(true)}}/><label className="application-consent"><input type="checkbox" disabled={busy} checked={question.required} onChange={event=>{setDraft({...draft,questions:draft.questions.map(q=>q.id===question.id?{...q,required:event.target.checked}:q)});setDirty(true)}}/>Required question {index+1}</label><div className="hero-actions"><button type="button" className="outline" disabled={busy||index===0} onClick={()=>move(index,-1)}>Move question {index+1} up</button><button type="button" className="outline" disabled={busy||index===draft.questions.length-1} onClick={()=>move(index,1)}>Move question {index+1} down</button><button type="button" className="outline" disabled={busy} onClick={()=>remove(index)}>Remove question {index+1}</button></div></fieldset>)}<div className="hero-actions">{removed&&<button type="button" className="outline" disabled={busy||draft.questions.length>=5} onClick={undoRemove}>Undo question removal</button>}<button type="button" className="outline" disabled={busy||draft.questions.length>=5} onClick={()=>{setDraft({...draft,questions:[...draft.questions,{id:crypto.randomUUID(),prompt:'',required:true}]});setDirty(true)}}>Add question</button><button disabled={busy||draft.questions.some(q=>!q.prompt.trim())}>Save application questions</button><button type="button" className="outline" disabled={busy} onClick={()=>{if(dirty&&!window.confirm('Discard unsaved question changes?'))return;setDraft(null);setRemoved(null);setDirty(false);setMessage('')}}>Cancel question changes</button></div></form>:form&&<><ol>{form.questions.map(q=><li key={q.id}>{q.prompt} · {q.required?'Required':'Optional'}</li>)}</ol>{!form.questions.length&&<p>No additional questions.</p>}<button className="outline" disabled={busy} onClick={()=>{setDraft(form);setMessage('')}}>Edit application questions</button></>}<button className="outline" disabled={busy} onClick={()=>void load()}>Reload application questions</button></section>;
}
