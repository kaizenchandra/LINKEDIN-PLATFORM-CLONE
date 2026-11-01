import {test,expect,Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
async function write(page:Page,url:string,data:unknown,method='put'){
 const csrf=await (await page.request.get('/api/v1/csrf')).json();
 return page.request.fetch(url,{method,headers:{[csrf.header]:csrf.token},data});
}
test('recruiter accepts delegated access and loses private application access on revocation',async({page,browser,request},testInfo)=>{
 test.setTimeout(120000);
 await register(page,request,'RecruitingOwner');
 const context=await browser.newContext(),candidateContext=await browser.newContext();
 try{
 const recruiter=await context.newPage(),candidate=await candidateContext.newPage();
 await register(recruiter,request,'RecruitingColleague');await register(candidate,request,'RecruitingApplicant');
 const company=crypto.randomUUID(),job=crypto.randomUUID(),connection=crypto.randomUUID();
 expect((await write(page,'/api/v1/organizations/'+company,{name:'Fictional Recruiting Studio',industry:'Technology',location:'Remote',about:'Test company',version:0})).ok()).toBeTruthy();
 expect((await write(page,'/api/v1/jobs/'+job,{organizationId:company,title:'Delegated engineering role',location:'Remote',description:'Fictional role',workplace:'REMOTE',status:'OPEN',version:0})).ok()).toBeTruthy();
 // Discovery consent is an explicit real API fixture.
 const visibility=await recruiter.request.get('/api/v1/me/visibility');
 expect(visibility.ok()).toBeTruthy();
 const preference=await visibility.json();
 expect((await write(recruiter,'/api/v1/me/visibility',{memberVisible:true,version:preference.version})).ok()).toBeTruthy();
 expect((await write(page,'/api/v1/network',{id:connection,recipientId:preference.publicId},'post')).ok()).toBeTruthy();
 expect((await write(recruiter,'/api/v1/network/'+connection,{action:'ACCEPT',version:0})).ok()).toBeTruthy();
 const submitted=await write(candidate,'/api/v1/jobs/'+job+'/application',{statement:'Private fictional application',resume:{fileName:'resume.txt',text:'Private fictional recruiter resume'}});
 expect(submitted.ok()).toBeTruthy();const application=await submitted.json();
 await page.goto('/recruiting?organization='+company);
 await page.getByLabel('Colleague').selectOption(connection);
 await page.getByRole('button',{name:'Send recruiter invitation',exact:true}).click();
 await expect(page.getByText('PENDING',{exact:true})).toBeVisible();
 expect((await recruiter.request.get('/api/v1/applications/'+application.id+'/history')).status()).toBe(404);
 expect((await recruiter.request.get('/api/v1/applications/'+application.id+'/resume')).status()).toBe(404);
 await recruiter.goto('/recruiting');recruiter.on('dialog',dialog=>dialog.accept());
 await recruiter.getByRole('button',{name:'Accept recruiter invitation',exact:true}).click();
 await expect(recruiter.getByText('ACTIVE',{exact:true})).toBeVisible();
 await recruiter.goto('/jobs/'+job);await recruiter.getByRole('button',{name:'Edit job',exact:true}).click();
 await recruiter.getByLabel('Job title',{exact:true}).fill('Recruiter updated role');
 await recruiter.getByRole('button',{name:'Save job changes',exact:true}).click();
 await expect(recruiter.getByRole('heading',{name:'Recruiter updated role',exact:true})).toBeVisible();
 await recruiter.goto('/applications?view=RECEIVED&job='+job);
 const resumeDownload=await recruiter.request.get('/api/v1/applications/'+application.id+'/resume');expect(resumeDownload.ok()).toBeTruthy();expect(await resumeDownload.text()).toBe('Private fictional recruiter resume');
 await recruiter.getByRole('button',{name:'Start review',exact:true}).click();
 await expect(recruiter.getByText('REVIEWING',{exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Refresh recruiter access',exact:true}).click();
 await expect(page.getByText('ACTIVE',{exact:true})).toBeVisible();
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.screenshot({path:testInfo.outputPath('recruiter-team-desktop.png'),fullPage:true});
 await page.setViewportSize({width:390,height:844});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.screenshot({path:testInfo.outputPath('recruiter-team-mobile.png'),fullPage:true});
 page.on('dialog',dialog=>dialog.accept());await page.getByRole('button',{name:'Revoke access',exact:true}).click();
 await expect(page.getByText('REVOKED',{exact:true})).toBeVisible();
 await expect(recruiter.getByRole('heading',{name:'No applications in this view'})).toBeVisible();
 expect((await recruiter.request.get('/api/v1/applications/'+application.id+'/history')).status()).toBe(404);
 expect((await recruiter.request.get('/api/v1/applications/'+application.id+'/resume')).status()).toBe(404);
 await recruiter.goto('/jobs/'+job);await expect(recruiter.getByRole('heading',{name:'Recruiter updated role',exact:true})).toBeVisible();
 await expect(recruiter.getByRole('button',{name:'Edit job',exact:true})).toHaveCount(0);
 }finally{await context.close();await candidateContext.close()}
});
