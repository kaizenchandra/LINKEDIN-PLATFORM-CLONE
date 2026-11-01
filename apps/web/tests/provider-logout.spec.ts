import {test,expect,Page} from '@playwright/test';
import {register} from './helpers/register';
test.use({trace:'off'});
async function login(page:Page,email:string){
 await page.goto('/');await page.getByRole('link',{name:'Sign in / Register',exact:true}).click();
 await expect(page.locator('input[name="password"]')).toBeVisible();
 const field=page.getByRole('textbox',{name:'Email',exact:true});if(await field.isVisible())await field.fill(email);
 await page.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
 await page.getByRole('button',{name:'Sign In',exact:true}).click();
 await expect(page.getByRole('heading',{name:'ProviderLogout Member',exact:true})).toBeVisible();
}
test('provider device logout revokes linked application sessions and closes live connections',async({page,request,browser})=>{
 const email=await register(page,request,'ProviderLogout');
 const otherContext=await browser.newContext();
 try{
  const other=await otherContext.newPage();await login(other,email);
  await other.goto('/notifications');await expect(other.getByText('Live updates connected.',{exact:true})).toBeVisible();
  await page.goto('/settings#security');await page.getByRole('link',{name:'identity provider sessions',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Device activity',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Sign out',exact:true}).click();
  await page.getByRole('button',{name:'Confirm',exact:true}).click();
  await expect.poll(async()=>(await other.request.get('/api/v1/me')).status(),{timeout:15000}).toBe(401);
  await expect(other.getByText('Live updates connected.',{exact:true})).toHaveCount(0,{timeout:10000});
  expect((await page.request.get('/api/v1/me')).status()).toBe(200);
  // A fresh authenticated login has a new provider SID and must remain usable.
  await login(other,email);expect((await other.request.get('/api/v1/me')).status()).toBe(200);
  await page.getByRole('button',{name:'Refresh the page',exact:true}).click();
  await page.getByRole('button',{name:'Sign out all devices',exact:true}).click();
  await page.getByRole('button',{name:'Confirm',exact:true}).click();
  await expect.poll(async()=>(await other.request.get('/api/v1/me')).status(),{timeout:15000}).toBe(401);
  await expect.poll(async()=>(await page.request.get('/api/v1/me')).status(),{timeout:15000}).toBe(401);
 }finally{await otherContext.close()}
});
