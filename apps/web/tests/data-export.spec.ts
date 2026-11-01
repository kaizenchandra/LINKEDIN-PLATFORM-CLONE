import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off',screenshot:'off',video:'off'});
test('recent authentication gates a durable owner-only export and cancellation',async({page,request,browser},testInfo)=>{
 const email=await register(page,request,'Export');
 await page.getByRole('button',{name:'Edit introduction',exact:true}).click();
 await page.getByRole('textbox',{name:'about',exact:true}).fill('My fictional export biography');
 await page.getByRole('button',{name:'Save profile',exact:true}).click();
 await expect(page.getByRole('status').first()).toHaveText('Profile saved.');
 await page.reload();
 await expect(page.getByText('My fictional export biography',{exact:true})).toBeVisible();
 await page.goto('/settings#privacy');
 await page.getByRole('link',{name:'Verify your identity',exact:true}).click();
 await expect(page).toHaveURL(/max_age=0/);
 if(await page.getByRole('textbox',{name:'Email',exact:true}).isVisible())await page.getByRole('textbox',{name:'Email',exact:true}).fill(email);
 await page.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
 await page.getByRole('button',{name:'Sign In',exact:true}).click();
 await expect(page).toHaveURL(/\/settings#privacy$/);
 await page.getByRole('button',{name:'Request data export',exact:true}).click();
 await expect(page.getByText('Ready to download',{exact:true})).toBeVisible({timeout:20000});
 await page.reload();await expect(page.getByRole('button',{name:'Download export',exact:true})).toBeVisible();
 const downloadPromise=page.waitForEvent('download');await page.getByRole('button',{name:'Download export',exact:true}).click();
 const download=await downloadPromise;expect(download.suggestedFilename()).toBe('meridian-business-data.json');
 const stream=await download.createReadStream();const chunks:Buffer[]=[];for await(const chunk of stream!)chunks.push(Buffer.from(chunk));
 const snapshot=JSON.parse(Buffer.concat(chunks).toString());expect(snapshot.schemaVersion).toBe(1);expect(snapshot.profile[0].about).toBe('My fictional export biography');
 expect(snapshot.spring_session).toBeUndefined();expect(snapshot.password).toBeUndefined();await download.delete();
 const exports=await (await page.request.get('/api/v1/me/exports')).json();const id=exports[0].id;
 const otherContext=await browser.newContext();try{
  const other=await otherContext.newPage();const otherEmail=await register(other,request,'ExportOther');
  // Establish a recent signed authentication timestamp in the second real account.
  await other.goto('/settings#privacy');await other.getByRole('link',{name:'Verify your identity',exact:true}).click();
  const emailField=other.getByRole('textbox',{name:'Email',exact:true});if(await emailField.isVisible())await emailField.fill(otherEmail);
  await other.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
  await other.getByRole('button',{name:'Sign In',exact:true}).click();await expect(other).toHaveURL(/\/settings#privacy$/);
  expect((await other.request.get(`/api/v1/me/exports/${id}/download`)).status()).toBe(404);
  // Listing must never contain the first account's request.
  await other.goto('http://localhost:3100/settings#privacy');
  const listing=await other.request.get('/api/v1/me/exports');expect(listing.ok()).toBeTruthy();expect(await listing.json()).toEqual([]);
 }finally{await otherContext.close()}
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.setViewportSize({width:390,height:844});await page.emulateMedia({colorScheme:'dark'});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.screenshot({path:testInfo.outputPath('data-privacy-mobile-dark.png')});
 page.once('dialog',dialog=>dialog.accept());await page.getByRole('button',{name:'Remove export',exact:true}).click();
 await expect(page.getByText('Expired or removed',{exact:true})).toBeVisible();await page.reload();
 await expect(page.getByRole('button',{name:'Download export',exact:true})).toHaveCount(0);
 expect((await page.request.get(`/api/v1/me/exports/${id}/download`)).status()).toBe(410);
});
