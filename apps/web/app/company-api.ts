import {messagingFetch,MessagingSessionExpired,MessagingTimeout} from './messaging-api';
export {MessagingSessionExpired as CompanySessionExpired};
export async function companyRequest(url:string,method='GET',body?:unknown,signal?:AbortSignal){
 try{
  const headers:Record<string,string>={};
  if(method!=='GET'){
   const token=await messagingFetch('/api/v1/csrf',{signal});if(!token.ok)throw Error('Unable to verify request. Retry.');
   const csrf=await token.json();headers[csrf.header]=csrf.token;headers['Content-Type']='application/json';
  }
  const response=await messagingFetch(url,{method,headers,body:body===undefined?undefined:JSON.stringify(body),cache:'no-store',signal});
  if(!response.ok)throw Error(response.status===409?'Company changed or request conflicts. Cancel edits and refresh before retrying.':response.status===403?'Only the page owner can manage this company.':response.status===404?'Company unavailable.':'Unable to confirm request. Refresh and retry.');
  return response;
 }catch(cause){if(cause instanceof MessagingTimeout)throw Error('Request timed out. Refresh to check the saved state before retrying.');throw cause}
}
