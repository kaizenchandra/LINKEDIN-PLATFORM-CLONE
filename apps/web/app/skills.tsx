'use client';
import {FormEvent} from 'react';
import {useProfileSection} from './use-profile-section';
import type {components} from './api-schema';
type Skill=components['schemas']['Skill'];
export default function Skills(){
 const {entries,draft,setDraft,message,setMessage,busy,load,mutate}=useProfileSection<Skill>('skills','Skill');
 function save(event:FormEvent){event.preventDefault();if(draft)void mutate('PUT','/api/v1/me/skills',draft);}
 return <section aria-label="Skills" className="experience"><h2>Skills</h2><p role="status">{message}</p>{draft?<form onSubmit={save}><label>Skill name<input required maxLength={80} value={draft.name} onChange={event=>setDraft({...draft,name:event.target.value})}/></label><button disabled={busy}>Save skill</button>{' '}<button type="button" disabled={busy} onClick={()=>{if(window.confirm('Discard unsaved skill changes?'))setDraft(null)}}>Cancel</button></form>:<><button disabled={busy} onClick={()=>setDraft({id:crypto.randomUUID(),name:'',version:0})}>Add skill</button>{entries.length===0&&<p>Add the capabilities you want to be known for.</p>}<ul>{entries.map(skill=><li key={skill.id}><strong>{skill.name}</strong>{' '}<button disabled={busy} onClick={()=>setDraft({...skill})}>Edit {skill.name}</button>{' '}<button disabled={busy} onClick={()=>{if(window.confirm(`Remove ${skill.name}?`))void mutate('DELETE',`/api/v1/me/skills/${skill.id}?version=${skill.version}`)}}>Remove {skill.name}</button></li>)}</ul><button disabled={busy} onClick={()=>load().catch(error=>setMessage(error.message))}>Reload skills</button></>}</section>;
}
