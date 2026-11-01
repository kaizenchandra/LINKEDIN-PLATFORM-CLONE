import {test,expect,Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
test('network suggestions, followers and mutual consent use persisted relationships',async({page,request,browser},testInfo)=>{
 test.setTimeout(90000);
 const suffix=Date.now(),aName='GraphA'+suffix,bName='GraphB'+suffix,cName='GraphC'+suffix;
 async function sharing(p:Page){
  const visibility=p.getByRole('region',{name:'Profile visibility',exact:true});await visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true}).click();await expect(visibility.getByText('Visibility saved.',{exact:true})).toBeVisible();
  const path=await visibility.getByRole('link',{name:'View member profile'}).getAttribute('href');
  const discovery=p.getByRole('region',{name:'People search visibility'});await discovery.getByRole('checkbox').click();await expect(discovery.getByRole('checkbox')).toBeChecked();await expect(discovery.getByRole('checkbox')).toBeEnabled();
  await p.goto('/settings#visibility');const pref=p.getByRole('region',{name:'Connection sharing'});await pref.getByRole('checkbox').click();await expect(pref.getByRole('status')).toHaveText('Connection privacy saved.');await p.reload();await expect(pref.getByRole('checkbox')).toBeChecked();return path!;
 }
 async function connect(sender:Page,receiver:Page,path:string,senderName:string){await sender.goto(path);
  const ids:string[]=[];if(senderName===aName)await sender.route('**/api/v1/network',async route=>{if(route.request().method()!=='POST')return route.continue();ids.push(route.request().postDataJSON().id);if(ids.length===1){const response=await route.fetch();expect(response.status()).toBe(204);await route.abort('failed');}else await route.continue();});
  await sender.getByRole('button',{name:'Connect',exact:true}).click();
  if(senderName===aName){await expect(sender.getByRole('button',{name:'Connect',exact:true})).toBeEnabled();await sender.getByRole('button',{name:'Connect',exact:true}).click();await expect.poll(()=>ids.length).toBe(2);expect(ids[0]).toBe(ids[1]);await sender.unroute('**/api/v1/network');}
await expect(sender.getByText('Connection request sent.',{exact:true})).toBeVisible();await receiver.goto('/network');const invite=receiver.getByRole('article').filter({has:receiver.getByRole('heading',{name:senderName+' Member',exact:true})});await invite.getByRole('button',{name:'Accept',exact:true}).click();await expect(receiver.getByText('Network updated.',{exact:true})).toBeVisible();}
 await register(page,request,aName);const aPath=await sharing(page);
 const bContext=await browser.newContext(),cContext=await browser.newContext();try{
  const b=await bContext.newPage(),c=await cContext.newPage();await register(b,request,bName);const bPath=await sharing(b);await register(c,request,cName);const cPath=await sharing(c);
  await connect(page,c,cPath,aName);await connect(b,c,cPath,bName);
  await page.goto(bPath);const mutual=page.getByRole('region',{name:'Mutual connections',exact:true});await expect(mutual.getByRole('heading',{name:cName+' Member',exact:true})).toBeVisible();
  await c.goto('/settings#visibility');await c.getByRole('region',{name:'Connection sharing'}).getByRole('checkbox').click();await expect(c.getByText('Connection privacy saved.',{exact:true})).toBeVisible();
  await mutual.getByRole('button',{name:'Refresh mutual connections',exact:true}).click();await expect(mutual.getByText('No shared mutual connections.',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Follow',exact:true}).click();await expect(page.getByRole('button',{name:'Unfollow',exact:true})).toBeVisible();
  await b.goto('/network');await b.getByRole('button',{name:'Followers',exact:true}).click();const followers=b.getByRole('region',{name:'Followers',exact:true});await expect(followers.getByRole('heading',{name:aName+' Member',exact:true})).toBeVisible();
  await page.goto('/network');const suggestions=page.getByRole('region',{name:'Suggested members',exact:true});const target=suggestions.getByRole('heading',{name:bName+' Member',exact:true});
  for(let i=0;i<30;i++){await expect(suggestions.getByRole('button',{name:'Refresh suggestions',exact:true})).toBeEnabled();if(await target.count())break;const more=suggestions.getByRole('button',{name:'More suggestions',exact:true});if(!await more.count())break;await more.click();}
  await expect(target).toBeVisible();await target.getByRole('link').click();await expect(page).toHaveURL(new RegExp(bPath+'$'));
  await page.getByRole('button',{name:'Connect',exact:true}).click();await expect(page.getByText('Connection request sent.',{exact:true})).toBeVisible();
  await page.goto('/network');await expect(suggestions.getByRole('button',{name:'Refresh suggestions',exact:true})).toBeEnabled();await expect(suggestions.getByRole('heading',{name:bName+' Member',exact:true})).toHaveCount(0);
  await page.setViewportSize({width:390,height:844});await page.emulateMedia({colorScheme:'dark'});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);await page.screenshot({path:testInfo.outputPath('network-discovery-mobile-dark.png'),fullPage:true});
 }finally{await bContext.close();await cContext.close()}
});
