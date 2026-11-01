import {messagingFetch,MessagingSessionExpired,MessagingTimeout} from './messaging-api';
export {MessagingSessionExpired as ApplicationSessionExpired};
export async function applicationRequest(url:string,method='GET',body?:unknown,signal?:AbortSignal){
 try{
  const headers:Record<string,string>={};
  if(method!=='GET'){
   const token=await messagingFetch('/api/v1/csrf',{signal});if(!token.ok)throw Error('Unable to verify request. Retry.');
   const csrf=await token.json();headers[csrf.header]=csrf.token;headers['Content-Type']='application/json';
  }
  const response=await messagingFetch(url,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',signal});
  if(!response.ok)throw Error(response.status===400?'Check your statement, answers and resume. Use a UTF-8 .txt file up to 20 KiB.':response.status===422?'Answer every required application question.':response.status===413?'Application is too large to submit.':response.status===409?'Application conflicts with current state. Check its status and reload application questions before retrying.':response.status===403?'You cannot perform this application action.':response.status===404?'Application or job unavailable.':'Unable to confirm request. Refresh and retry.');
  return response;
 }catch(cause){if(cause instanceof MessagingTimeout)throw Error('Request timed out. Refresh to check the saved state before retrying.');throw cause}
}
