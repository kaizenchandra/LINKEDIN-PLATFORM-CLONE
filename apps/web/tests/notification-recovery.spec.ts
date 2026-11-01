import {test,expect} from '@playwright/test';
import {register} from './helpers/register';
test.use({trace:'off'});
test('notification refresh recovers from delayed transport and clears a revoked session',async({page,request})=>{
 test.setTimeout(60000);
 await register(page,request,'NotificationRecovery');
 let release:()=>void=()=>{};
 let intercepted=false;
 try{
  await page.route('**/api/v1/notifications?*',async route=>{
   if(intercepted){await route.continue();return}intercepted=true;
   const response=await route.fetch();await new Promise<void>(resolve=>{release=resolve});
   try{await route.fulfill({response})}catch{/* The UI deadline may already have cancelled this real response. */}
  });
  await page.goto('http://localhost:3100/notifications');
  await expect(page.getByRole('status')).toHaveText('Updates timed out. Refresh or wait for recovery.',{timeout:15000});
  release();await page.unroute('**/api/v1/notifications?*');
  await page.getByRole('button',{name:'Refresh notifications',exact:true}).click();
  await expect(page.getByRole('heading',{name:'You’re all caught up'})).toBeVisible();
  await page.context().setOffline(true);
  await expect(page.getByText('Live updates disconnected. Polling continues.',{exact:true})).toBeVisible();
  await page.context().setOffline(false);
  await expect(page.getByText('Live updates connected.',{exact:true})).toBeVisible({timeout:30000});
  await expect(page.getByRole('checkbox',{name:'New connection notifications'})).toBeEnabled();
  const csrf=await (await page.request.get('/api/v1/csrf')).json();
  const logout=await page.request.post('/api/v1/logout',{headers:{[csrf.header]:csrf.token},maxRedirects:0});
  expect([200,204,302,303]).toContain(logout.status());
  await expect(page.getByRole('status')).toHaveText('Session expired. Sign in again.',{timeout:15000});
  await expect(page.getByText('Live updates stopped.',{exact:true})).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(0);
  await expect(page.getByRole('checkbox',{name:'New connection notifications'})).toBeDisabled();
  await expect(page.getByRole('button',{name:'Refresh notifications',exact:true})).toBeDisabled();
  await expect(page.getByRole('link',{name:'Sign in / Register',exact:true})).toBeVisible();
 }finally{release();await page.context().setOffline(false)}
});
