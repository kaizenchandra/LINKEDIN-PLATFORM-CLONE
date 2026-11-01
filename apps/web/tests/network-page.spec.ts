import {test,expect,Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
async function write(page:Page,url:string,data:unknown,method='PUT'){const csrf=await (await page.request.get('/api/v1/csrf')).json();const result=await page.request.fetch(url,{method,headers:{[csrf.header]:csrf.token},data});expect(result.ok()).toBeTruthy()}
test('network navigation shows real invitations and persists acceptance and removal',async({page,request,browser},info)=>{
 await register(page,request,'NetworkRecipient');const visibility=await (await page.request.get('/api/v1/me/visibility')).json();
 await write(page,'/api/v1/me/visibility',{memberVisible:true,version:visibility.version});
 const context=await browser.newContext();try{
 const sender=await context.newPage();await register(sender,request,'NetworkSender');
 await write(sender,'/api/v1/network',{id:crypto.randomUUID(),recipientId:visibility.publicId},'POST');
 await page.getByRole('link',{name:'My Network',exact:true}).click();await expect(page).toHaveURL(/\/network$/);
 await expect(page.getByRole('link',{name:'My Network',exact:true})).toHaveAttribute('aria-current','page');
 await expect(page.getByRole('heading',{name:'Invitations (1)',exact:true})).toBeVisible();
 await expect(page.getByRole('heading',{name:'NetworkSender Member',exact:true})).toBeVisible();
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.screenshot({path:info.outputPath('network-desktop.png'),fullPage:true});
 await page.setViewportSize({width:390,height:844});await page.emulateMedia({colorScheme:'dark'});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.screenshot({path:info.outputPath('network-mobile-dark.png'),fullPage:true});
 await page.getByRole('button',{name:'Accept',exact:true}).click();await expect(page.getByRole('heading',{name:'Invitations (0)',exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Connections 1',exact:true}).click();await expect(page.getByRole('heading',{name:'Connections (1)',exact:true})).toBeVisible();
 await page.reload();await page.getByRole('button',{name:'Connections 1',exact:true}).click();
 await page.getByLabel('Search by name').fill('No match');await expect(page.getByText('No matching members.',{exact:true})).toBeVisible();await page.getByLabel('Search by name').fill('');
 page.once('dialog',dialog=>dialog.accept());await page.getByRole('button',{name:'Remove connection',exact:true}).click();await expect(page.getByRole('heading',{name:'Connections (0)',exact:true})).toBeVisible();
 }finally{await context.close()}
});
