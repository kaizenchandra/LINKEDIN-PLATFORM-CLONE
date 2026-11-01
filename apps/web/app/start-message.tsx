'use client';
import {useState} from 'react';
import {messagingWrite} from './messaging-api';
export default function StartMessage({memberId,compact=false}:{memberId:string;compact?:boolean}){
 const [busy,setBusy]=useState(false),[message,setMessage]=useState('');
 async function start(){if(busy)return;setBusy(true);setMessage('');try{const result=await messagingWrite('/api/v1/conversations','POST',{memberId});const conversation=await result.json();window.location.assign('/messages?conversation='+encodeURIComponent(conversation.id))}catch(error){setMessage((error as Error).message);setBusy(false)}}
 return <div><button className="outline" disabled={busy} onClick={()=>void start()}>Message</button>{!compact&&<p className="muted">Accepted connections only</p>}<p aria-live="polite">{message}</p></div>;
}
