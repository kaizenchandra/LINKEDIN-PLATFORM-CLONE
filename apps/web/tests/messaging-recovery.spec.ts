import {test,expect,Page} from '@playwright/test';
import {register} from './helpers/register';
test.use({trace:'off'});
async function write(page:Page,path:string,method:string,data:unknown){
 const csrf=await (await page.request.get('/api/v1/csrf')).json();
 const response=await page.request.fetch(path,{method,data,headers:{[csrf.header]:csrf.token}});
 expect(response.ok()).toBeTruthy();return response;
}
async function connect(a:Page,b:Page){
 const visibility=await (await b.request.get('/api/v1/me/visibility')).json();
 await write(b,'/api/v1/me/visibility','PUT',{memberVisible:true,version:visibility.version});
 const id=crypto.randomUUID();
 await write(a,'/api/v1/network','POST',{id,recipientId:visibility.publicId});
 await write(b,'/api/v1/network/'+id,'PUT',{action:'ACCEPT',version:0});
 return (await (await write(a,'/api/v1/conversations','POST',{memberId:visibility.publicId})).json()).id as string;
}
test('message refresh deadlines, stale responses and real session revocation',async({page,request,browser})=>{
 test.setTimeout(90000);
 const second=await browser.newContext(),third=await browser.newContext();
 const release: Array<()=>void>=[];
 try{
  await register(page,request,'RecoveryA');
  const b=await second.newPage(),c=await third.newPage();
  await register(b,request,'RecoveryB');await register(c,request,'RecoveryC');
  // Fixture setup uses real authenticated domain APIs; navigation/history below use the UI.
  const first=await connect(page,b),other=await connect(page,c);
  await write(b,'/api/v1/conversations/'+first+'/messages/'+crypto.randomUUID(),'PUT',{text:'First private conversation'});
  await write(c,'/api/v1/conversations/'+other+'/messages/'+crypto.randomUUID(),'PUT',{text:'Second private conversation'});
  const foreignOrigin=await page.context().newPage();await foreignOrigin.goto('http://localhost:8081/readyz');
  const opened=await foreignOrigin.evaluate(()=>new Promise<boolean>(resolve=>{
   const ws=new WebSocket('ws://localhost:8081/api/v1/realtime');const timer=setTimeout(()=>{ws.close();resolve(false)},4000);
   ws.onopen=()=>{clearTimeout(timer);ws.close();resolve(true)};ws.onerror=()=>{clearTimeout(timer);resolve(false)};
  }));
  expect(opened).toBe(false);await foreignOrigin.close();
  await page.goto('/messages?conversation='+first);
  await expect(page.getByText('Live updates connected.',{exact:true})).toBeVisible();

  await expect(page.getByText('First private conversation',{exact:true})).toBeVisible();
  const search=page.getByRole('textbox',{name:'Find conversations by name',exact:true});
  await page.getByRole('textbox',{name:'Message',exact:true}).fill('Keep this draft while searching');
  await search.fill('recoveryc');await page.getByRole('button',{name:'Search conversations',exact:true}).click();
  await expect(page.getByRole('button',{name:/RecoveryC Member/})).toBeVisible();
  await expect(page.getByRole('button',{name:/RecoveryB Member/})).toHaveCount(0);
  await expect(page.getByRole('textbox',{name:'Message',exact:true})).toHaveValue('Keep this draft while searching');
  await search.fill('No matching counterpart');await page.getByRole('button',{name:'Search conversations',exact:true}).click();
  await expect(page.getByText('No conversations match this name in the selected view.',{exact:true})).toBeVisible();
  await expect(page.getByText('First private conversation',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Clear search',exact:true}).click();
  await expect(page.getByRole('button',{name:/RecoveryB Member/})).toBeVisible();
  await expect(page.getByRole('button',{name:/RecoveryC Member/})).toBeVisible();
  await page.getByRole('textbox',{name:'Message',exact:true}).fill('');

  let captured=false;const held=new Promise<void>(resolve=>release.push(resolve));
  await page.route('**/api/v1/conversations/'+first+'/messages',async route=>{
   try{const response=await route.fetch();captured=true;await held;await route.fulfill({response})}catch{/* Browser cancellation is expected. */}
  });
  await page.getByRole('button',{name:'Refresh messages',exact:true}).click();
  await expect.poll(()=>captured).toBe(true);
  await page.getByRole('button',{name:/RecoveryC Member/}).click();
  await expect(page.getByText('Second private conversation',{exact:true})).toBeVisible();
  release[0]();
  await page.unroute('**/api/v1/conversations/'+first+'/messages');
  await expect(page.getByText('First private conversation',{exact:true})).toHaveCount(0);
  // A real history response is held past the client deadline, then recovery fetches storage.
  const stalled=new Promise<void>(resolve=>release.push(resolve));
  await page.route('**/api/v1/conversations/'+other+'/messages',async route=>{
   try{const response=await route.fetch();await stalled;await route.fulfill({response})}catch{/* Deadline cancellation is expected. */}
  });
  await page.getByRole('button',{name:'Refresh messages',exact:true}).click();
  await expect(page.getByRole('status')).toHaveText('Updates timed out. Retrying automatically.',{timeout:16000});
  release[1]();await page.unroute('**/api/v1/conversations/'+other+'/messages');
  await page.getByRole('button',{name:'Refresh messages',exact:true}).click();
  await expect(page.getByRole('status')).toHaveText('Updates every 3 seconds.');
  await page.getByRole('textbox',{name:'Message',exact:true}).fill('Unsent private draft');
  const csrf=await (await page.request.get('/api/v1/csrf')).json();
  const logout=await page.request.post('/api/v1/logout',{headers:{[csrf.header]:csrf.token},maxRedirects:0});
  expect([200,302,303]).toContain(logout.status());
  await page.getByRole('button',{name:'Refresh messages',exact:true}).click();
  await expect(page.getByRole('status')).toHaveText('Session expired. Sign in again.');
  await expect(page.getByText('Second private conversation',{exact:true})).toHaveCount(0);
  await expect(page.getByRole('textbox',{name:'Message',exact:true})).toHaveCount(0);
  await expect(page.getByRole('button',{name:/RecoveryC Member/})).toHaveCount(0);
  await expect(page.getByRole('link',{name:'Sign in / Register',exact:true})).toBeVisible();
  expect((await page.request.get('/api/v1/conversations')).status()).toBe(401);
  await expect(page.getByText('Live updates stopped.',{exact:true})).toBeVisible();
  const anonymous=await browser.newContext();try{
   const guest=await anonymous.newPage();await guest.goto('http://localhost:3100');
   expect(await guest.evaluate(()=>new Promise<boolean>(resolve=>{
    const ws=new WebSocket('ws://localhost:8081/api/v1/realtime');const timer=setTimeout(()=>{ws.close();resolve(false)},4000);
    ws.onopen=()=>{clearTimeout(timer);ws.close();resolve(true)};ws.onerror=()=>{clearTimeout(timer);resolve(false)};
   }))).toBe(false);
  }finally{await anonymous.close()}

 }finally{release.forEach(resolve=>resolve());await page.unrouteAll({behavior:'ignoreErrors'});await second.close();await third.close()}
});
