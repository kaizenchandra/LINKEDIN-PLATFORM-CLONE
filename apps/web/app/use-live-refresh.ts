'use client';
import {useEffect,useRef,useState} from 'react';
/** Content-free hints only. Callers own authorized REST reads and polling recovery. */
export function useLiveRefresh(refresh:()=>Promise<unknown>,stopped=false){
 const socket=useRef<WebSocket|null>(null),refreshFromSignal=useRef(refresh),expired=useRef(stopped);
 expired.current=stopped;
 const [realtime,setRealtime]=useState('Connecting live updates…');
 refreshFromSignal.current=refresh;
 useEffect(()=>{
  if(stopped){setRealtime('Live updates stopped.');return;}
  let disposed=false,timer:ReturnType<typeof setTimeout>,delay=1000;
  function connect(){
   if(disposed||expired.current||socket.current?.readyState===WebSocket.OPEN||socket.current?.readyState===WebSocket.CONNECTING)return;
   const local=window.location.hostname==='localhost'&&window.location.port==='3100';
   const url=process.env.NEXT_PUBLIC_REALTIME_URL||(local?'ws://localhost:8081/api/v1/realtime':(window.location.protocol==='https:'?'wss://':'ws://')+window.location.host+'/api/v1/realtime');
   const ws=new WebSocket(url);socket.current=ws;
   ws.onopen=()=>{if(disposed||expired.current){ws.close();return}delay=1000;setRealtime('Live updates connected.');void refreshFromSignal.current()};
   ws.onmessage=event=>{if(disposed||expired.current)return;try{const data=JSON.parse(event.data);if(data.version===1&&data.type==='refresh')void refreshFromSignal.current()}catch{/* Unsupported frames never change business state. */}};
   ws.onerror=()=>ws.close();
   ws.onclose=()=>{if(socket.current===ws)socket.current=null;if(disposed||expired.current)return;setRealtime('Live updates disconnected. Polling continues.');void refreshFromSignal.current();const wait=delay+Math.random()*500;delay=Math.min(delay*2,30000);timer=setTimeout(connect,wait)};
  }
  const online=()=>{clearTimeout(timer);connect();void refreshFromSignal.current()};
  const offline=()=>socket.current?.close();
  connect();window.addEventListener('online',online);window.addEventListener('offline',offline);
  return()=>{disposed=true;clearTimeout(timer);socket.current?.close();socket.current=null;window.removeEventListener('online',online);window.removeEventListener('offline',offline)};
 },[stopped]);
 return realtime;
}
