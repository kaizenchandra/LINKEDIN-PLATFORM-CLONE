export class MessagingSessionExpired extends Error {
 constructor(){super('Session expired. Sign in again.')}
}
export class MessagingTimeout extends Error {
 constructor(){super('Request timed out. Check history before retrying.')}
}
/** Deadline covers response headers and body. Only explicit caller actions retry writes. */
export async function messagingFetch(url:string,init:RequestInit={}){
 const controller=new AbortController();let timedOut=false;
 const cancel=()=>controller.abort();
 if(init.signal?.aborted)cancel();else init.signal?.addEventListener('abort',cancel,{once:true});
 const timer=setTimeout(()=>{timedOut=true;controller.abort()},10000);
 try{
  const response=await fetch(url,{...init,signal:controller.signal});
  if(response.status===401)throw new MessagingSessionExpired();
  const body=await response.arrayBuffer();
  return new Response(response.status===204?null:body,{status:response.status,statusText:response.statusText,headers:response.headers});
 }catch(error){if(timedOut)throw new MessagingTimeout();throw error}
 finally{clearTimeout(timer);init.signal?.removeEventListener('abort',cancel)}
}
export async function messagingWrite(url:string,method:string,body:unknown,signal?:AbortSignal){
 const token=await messagingFetch('/api/v1/csrf',{signal});if(!token.ok)throw Error('Unable to verify this request. Retry.');
 const csrf=await token.json();const result=await messagingFetch(url,{method,signal,headers:{'Content-Type':'application/json',[csrf.header]:csrf.token},body:JSON.stringify(body)});
 if(!result.ok)throw Error(result.status===404?'Messaging requires an accepted connection and no block.':result.status===409?(url.endsWith('/preferences')?'Conversation settings changed. Refresh before retrying.':'Message ID conflict. Check history before editing and retrying.'):'Change could not be confirmed. Check history and retry.');
 return result;
}
