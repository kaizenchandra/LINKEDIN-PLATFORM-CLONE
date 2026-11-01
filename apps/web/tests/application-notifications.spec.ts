import {test,expect,Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
async function write(page:Page,url:string,data:unknown){const csrf=await (await page.request.get('/api/v1/csrf')).json();const result=await page.request.put(url,{headers:{[csrf.header]:csrf.token},data});expect(result.ok()).toBeTruthy();return result}
test('application events produce private persistent job notifications and preference changes',async({page,request,browser},testInfo)=>{
 await register(page,request,'JobNotificationOwner');const company=crypto.randomUUID(),job=crypto.randomUUID();
 await write(page,'/api/v1/organizations/'+company,{name:'Fictional Notification Company',industry:'Technology',location:'Remote',about:'Fictional organization',version:0});
 await write(page,'/api/v1/jobs/'+job,{organizationId:company,title:'Notification engineer',location:'Remote',description:'Fictional job',workplace:'REMOTE',status:'OPEN',version:0});
 const context=await browser.newContext();
 try{
  const applicant=await context.newPage();await register(applicant,request,'JobNotificationApplicant');
  await page.goto('/notifications');await page.getByRole('button',{name:'Jobs',exact:true}).click();
  await applicant.goto('/jobs/'+job);await applicant.getByLabel('Application statement',{exact:true}).fill('Fictional private application statement.');
  await applicant.getByRole('checkbox',{name:'Share these application details with this company'}).check();await applicant.getByRole('button',{name:'Submit application',exact:true}).click();
  await expect(applicant.getByText('SUBMITTED',{exact:true})).toBeVisible();
  const incoming=page.getByRole('article',{name:'Notification from Fictional Notification Company'});
  await expect(incoming).toHaveCount(1);await expect(incoming).toContainText('Application submitted.');
  await expect(incoming).not.toContainText('private application statement');
  await page.reload();await expect(incoming).toHaveCount(1);
  await incoming.getByRole('button',{name:'Mark read',exact:true}).click();await expect(page.getByText('Unread: 0',{exact:true})).toBeVisible();
  await incoming.getByRole('button',{name:'Mark unread',exact:true}).click();await expect(page.getByText('Unread: 1',{exact:true})).toBeVisible();
  await page.reload();await expect(incoming.getByRole('button',{name:'Mark read',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Mark all read',exact:true}).click();await expect(page.getByText('Unread: 0',{exact:true})).toBeVisible();
  await incoming.getByRole('link',{name:'View application',exact:true}).click();await expect(page.getByRole('heading',{name:'Received applications',exact:true})).toBeVisible();
  await applicant.goto('/notifications');await applicant.getByRole('button',{name:'Jobs',exact:true}).click();
  await page.getByRole('button',{name:'Start review',exact:true}).click();
  const update=applicant.getByRole('article',{name:'Notification from Fictional Notification Company'});
  await expect(update).toHaveCount(1);await expect(update).toContainText('Application moved to review.');await expect(applicant.getByText('Unread: 1',{exact:true})).toBeVisible();
  await applicant.getByRole('button',{name:'Network',exact:true}).click();await expect(applicant.getByRole('heading',{name:'You’re all caught up'})).toBeVisible();await expect(applicant.getByText('Unread: 1',{exact:true})).toBeVisible();
  await applicant.getByRole('button',{name:'Jobs',exact:true}).click();await expect(update).toHaveCount(1);
  expect((await new AxeBuilder({page:applicant}).analyze()).violations).toEqual([]);
  await applicant.screenshot({path:testInfo.outputPath('job-notifications-desktop.png'),fullPage:true});
  await applicant.setViewportSize({width:390,height:844});await applicant.emulateMedia({colorScheme:'dark'});
  expect(await applicant.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  expect((await new AxeBuilder({page:applicant}).analyze()).violations).toEqual([]);
  await applicant.evaluate(()=>window.scrollTo(0,0));await applicant.screenshot({path:testInfo.outputPath('job-notifications-mobile-dark.png'),fullPage:true});
  await update.getByRole('button',{name:'Mark read',exact:true}).click();await expect(applicant.getByRole('status')).toContainText('Notification marked read.');await applicant.reload();await expect(update).toHaveCount(1);await expect(applicant.getByText('Unread: 0',{exact:true})).toBeVisible();
  const preference=applicant.getByRole('checkbox',{name:'Job application notifications',exact:true});await expect(preference).toBeEnabled();await preference.click();await expect(applicant.getByRole('status')).toContainText('Notification preferences saved.');await applicant.reload();await expect(preference).not.toBeChecked();
  await expect(applicant.getByRole('checkbox',{name:'New connection notifications',exact:true})).toBeChecked();
  await update.getByRole('link',{name:'View application',exact:true}).click();await expect(applicant.getByRole('heading',{name:'My applications',exact:true})).toBeVisible();await expect(applicant.getByText('REVIEWING',{exact:true})).toBeVisible();
  await applicant.goto('/notifications');await expect(update).toHaveCount(1);
  // The applicant owns this notice; dismissal is confirmed in their own browser.
  applicant.once('dialog',dialog=>dialog.dismiss());await update.getByRole('button',{name:'Dismiss',exact:true}).click();await expect(update).toHaveCount(1);
  applicant.once('dialog',dialog=>dialog.accept());await update.getByRole('button',{name:'Dismiss',exact:true}).click();await expect(update).toHaveCount(0);
  await applicant.reload();await expect(update).toHaveCount(0);await expect(applicant.getByText('Unread: 0',{exact:true})).toBeVisible();
 }finally{await context.close()}
});
