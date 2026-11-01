'use client';
import {FormEvent,useEffect,useRef,useState} from 'react';
import ResumeDownload from './resume-download';
import {applicationRequest,ApplicationSessionExpired} from './application-api';
import type {components} from './api-schema';
type Job=components['schemas']['Job'];type Application=components['schemas']['JobApplication'];
export default function ApplyToJob({job}:{job:Job}){
 const [receipt,setReceipt]=useState<Application|null>(null),[profile,setProfile]=useState<{name:string;headline:string}|null>(null),[statement,setStatement]=useState(''),[consent,setConsent]=useState(false),[busy,setBusy]=useState(true),[expired,setExpired]=useState(false),[message,setMessage]=useState('Loading application…');
 const [resume,setResume]=useState<components['schemas']['ResumeSubmission']|null>(null),[resumeError,setResumeError]=useState('');
 const [questions,setQuestions]=useState<components['schemas']['JobQuestionForm']|null>(null),[answers,setAnswers]=useState<Record<string,string>>({}),[reload,setReload]=useState(0);
 const fileInput=useRef<HTMLInputElement|null>(null),fileRead=useRef(0);
 const dirty=useRef(false);dirty.current=(!!statement.trim()||!!resume||Object.values(answers).some(value=>!!value.trim()))&&!receipt;
 useEffect(()=>()=>{fileRead.current++},[]);
 async function selectResume(file:File|undefined){
  const generation=++fileRead.current;setResume(null);setResumeError('');setConsent(false);if(!file)return;setBusy(true);
  try{
   if(!file.name.toLowerCase().endsWith('.txt')||file.name.length>120||/[\\/\x00-\x1f\x7f]/.test(file.name)||file.size>20480)throw Error('Choose a UTF-8 .txt resume up to 20 KiB.');
   const text=new TextDecoder('utf-8',{fatal:true}).decode(await file.arrayBuffer());
   if(!text.trim()||/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f-\x9f]/.test(text))throw Error('Resume must contain plain text without binary control characters.');
   if(generation===fileRead.current)setResume({fileName:file.name,text});
  }catch(cause){if(generation===fileRead.current)setResumeError(cause instanceof TypeError?'Resume must use valid UTF-8 encoding.':(cause as Error).message)}
  finally{if(generation===fileRead.current)setBusy(false)}
 }
 function removeResume(){fileRead.current++;setResume(null);setResumeError('');setConsent(false);if(fileInput.current)fileInput.current.value=''}

 function fail(cause:unknown){if(cause instanceof ApplicationSessionExpired){setExpired(true);setProfile(null);setReceipt(null);setStatement('');setResume(null);setResumeError('');setAnswers({});setQuestions(null);setConsent(false)}setMessage((cause as Error).message)}
 useEffect(()=>{const controller=new AbortController();let disposed=false;
  void Promise.all([applicationRequest('/api/v1/applications?job='+job.id,'GET',undefined,controller.signal),applicationRequest('/api/v1/me','GET',undefined,controller.signal),applicationRequest('/api/v1/jobs/'+job.id+'/questions','GET',undefined,controller.signal)]).then(async([response,me,questionResponse])=>{const data:components['schemas']['ApplicationPage']=await response.json(),account=await me.json(),questionSet=await questionResponse.json();if(!disposed){setReceipt(data.items[0]||null);setProfile(account);setQuestions(questionSet);setMessage('')}}).catch(cause=>{if(!disposed)fail(cause)}).finally(()=>{if(!disposed)setBusy(false)});
  return()=>{disposed=true;controller.abort()};
 },[job.id,reload]);
 useEffect(()=>{const warn=(event:BeforeUnloadEvent)=>{if(dirty.current)event.preventDefault()};window.addEventListener('beforeunload',warn);return()=>window.removeEventListener('beforeunload',warn)},[]);
 async function reloadQuestions(){
  if(Object.values(answers).some(value=>value.trim())&&!window.confirm('Reload questions? Answers to changed or removed questions will be cleared.'))return;
  setBusy(true);
  try{const next:components['schemas']['JobQuestionForm']=await (await applicationRequest('/api/v1/jobs/'+job.id+'/questions')).json();
   setAnswers(previous=>Object.fromEntries(next.questions.filter(q=>questions?.questions.some(old=>old.id===q.id&&old.prompt===q.prompt&&old.required===q.required)).map(q=>[q.id,previous[q.id]||''])));
   setQuestions(next);setConsent(false);setMessage('Questions reloaded. Review your answers before submitting.');
  }catch(cause){fail(cause)}finally{setBusy(false)}
 }
 async function submit(event:FormEvent){event.preventDefault();if(busy||expired||!consent||!profile||!questions||!!resumeError)return;setBusy(true);setMessage('Submitting application…');
  try{const result:Application=await (await applicationRequest('/api/v1/jobs/'+job.id+'/application','PUT',{statement,questionVersion:questions.version,answers:questions.questions.map(q=>({questionId:q.id,text:answers[q.id]||''})),...(resume?{resume}:{})})).json();dirty.current=false;setReceipt(result);setStatement('');setResume(null);setAnswers({});setMessage('Application persisted. Track its status in My applications.')}catch(cause){fail(cause)}finally{setBusy(false)}
 }
 if(job.canManage)return <section className="profile-card"><h2>Applications</h2><p><a href={'/applications?view=RECEIVED&job='+job.id}>Review applications</a></p></section>;
 return <section className="profile-card" aria-label="Job application"><h2>Apply with your profile</h2><p role="status">{message}</p>{!profile&&!busy&&!expired&&<button className="outline" onClick={()=>{setBusy(true);setReload(value=>value+1)}}>Retry application loading</button>}{receipt?<><p>Application status: <strong>{receipt.status}</strong></p><p>Your submitted statement and profile snapshot are stored. Submitting again does not create a new application.</p>{receipt.hasResume&&<ResumeDownload applicationId={receipt.id} onExpired={()=>fail(new ApplicationSessionExpired())}/>}<a href={'/applications?job='+job.id}>Track my application</a></>:job.status==='CLOSED'?<p>This job is closed to new applications.</p>:profile&&!expired?<form className="company-form" onSubmit={submit}><p>The company owner and authorized recruiters will receive your name ({profile.name}), headline ({profile.headline||'not provided'}) and statement, answers to application questions and any resume you attach. Your email and private profile sections are not included automatically; anything you put in the resume is shared.</p><label htmlFor="application-resume">Resume (optional .txt)</label><input ref={fileInput} id="application-resume" type="file" accept=".txt,text/plain" aria-describedby="resume-help" disabled={busy} onChange={event=>void selectResume(event.target.files?.[0])}/><p id="resume-help" className="muted">UTF-8 plain text, up to 20 KiB. PDF and Word files are not supported yet. The resume is saved only when you submit and cannot be replaced afterward.</p>{resumeError&&<p role="alert">{resumeError}</p>}{resume&&<details><summary>Preview selected resume</summary><p className="preserve-lines resume-preview">{resume.text}</p></details>}{(resume||resumeError)&&<button type="button" className="outline" disabled={busy} onClick={removeResume}>Remove resume</button>}<label htmlFor="application-statement">Application statement</label><textarea id="application-statement" required maxLength={4000} rows={6} disabled={busy} value={statement} onChange={event=>setStatement(event.target.value)}/>{questions?.questions.map(q=><div key={q.id}><label htmlFor={'answer-'+q.id}>{q.prompt} ({q.required?'Required':'Optional'})</label><textarea id={'answer-'+q.id} required={q.required} maxLength={1000} rows={3} disabled={busy} value={answers[q.id]||''} onChange={event=>setAnswers({...answers,[q.id]:event.target.value})}/></div>)}<button type="button" className="outline" disabled={busy} onClick={()=>void reloadQuestions()}>Reload application questions</button><label className="application-consent"><input type="checkbox" required checked={consent} disabled={busy} onChange={event=>setConsent(event.target.checked)}/> Share these application details with this company</label><button disabled={busy||!consent||!statement.trim()||!!resumeError||!questions||questions.questions.some(q=>q.required&&!answers[q.id]?.trim())}>Submit application</button></form>:null}<p className="muted">One application per job. You may withdraw while it is submitted or under review; reapplication is not supported yet.</p></section>;
}
