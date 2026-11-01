import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';

test('shared design covers product pages at desktop, mobile and dark sizes',async({page,request},testInfo)=>{
 test.setTimeout(240000);
 await register(page,request,'Design');
 const routes=[['/feed','Home feed'],['/network','Invitations (0)'],['/jobs','Discover jobs'],['/messages','Messaging'],['/notifications','Notifications'],['/search','Find people'],['/companies','Companies'],['/applications','My applications'],['/settings','Settings'],['/recruiting','Recruiter access']] as const;
 for(const mode of ['desktop','mobile','dark'] as const){
  await page.setViewportSize(mode==='mobile'?{width:390,height:844}:{width:1440,height:1000});
  await page.emulateMedia({colorScheme:mode==='dark'?'dark':'light',reducedMotion:'reduce'});
  await page.evaluate(()=>localStorage.setItem('meridian-theme','system'));
  for(const [path,heading] of routes){
   await page.goto(path);
   await expect(page.getByRole('heading',{name:heading,exact:true})).toBeVisible();
   await expect(page.locator('.account-menu').first()).toBeVisible();
   if(mode==='mobile'&&path==='/jobs'){
    const box=await page.getByRole('heading',{name:'Discover jobs',exact:true}).boundingBox();
    expect(box!.y).toBeLessThan(600);
   }
   expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),`${mode} ${path} overflows`).toBe(true);
   const accessibility=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze();
   expect(accessibility.violations,`${mode} ${path}`).toEqual([]);
   await page.screenshot({path:testInfo.outputPath(`${path.slice(1)}-${mode}.png`)});
  }
 }
 await page.setViewportSize({width:1440,height:1000});
 await page.goto('/feed');
 await page.locator('.business-menu summary').click();
 await page.getByRole('link',{name:'Company pages',exact:true}).click();
 await expect(page).toHaveURL(/\/companies$/);
});
