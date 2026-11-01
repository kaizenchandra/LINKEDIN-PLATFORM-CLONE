import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
test('following persists independently, masks private profiles and respects blocks',async({page,request,browser})=>{
 await register(page,request,'Followee');
 const visibility=page.getByRole('region',{name:'Profile visibility',exact:true});
 await visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true}).click();
 await expect(visibility.getByText('Visibility saved.',{exact:true})).toBeVisible();
 const path=await visibility.getByRole('link',{name:'View member profile'}).getAttribute('href');
 const publicPost='Following feed update '+Date.now(),privatePost='Private following update '+Date.now();
 await page.goto('/feed');
 for(const [text,audience] of [[publicPost,'MEMBERS'],[privatePost,'ONLY_ME']]){
  await page.getByRole('button',{name:'Start a post',exact:true}).click();
  await page.getByRole('textbox',{name:'Post text',exact:true}).fill(text);
  await page.getByRole('combobox',{name:'Post visibility'}).selectOption(audience);
  await page.getByRole('button',{name:'Publish post',exact:true}).click();
  await expect(page.getByRole('dialog')).not.toBeVisible();
 }
 await page.goto('/');
 const context=await browser.newContext();
 try{
  const follower=await context.newPage();await register(follower,request,'Follower');
  await follower.goto('http://localhost:3100'+path);
  const follow=follower.getByRole('button',{name:'Follow',exact:true});
  await expect(follow).toBeEnabled();
  // Explicit transport failure injection; successful flows use the real backend.
  await follower.route('**/api/v1/following/*',async route=>route.request().method()==='PUT'?route.abort('failed'):route.continue());
  await follow.click();
  await expect(follower.getByText('Follow change could not be confirmed. Refresh status before retrying.',{exact:true})).toBeVisible();
  await expect(follow).toBeVisible();await follower.unroute('**/api/v1/following/*');
  await follower.getByRole('button',{name:'Refresh follow status',exact:true}).click();
  await follow.click();await expect(follower.getByRole('button',{name:'Unfollow',exact:true})).toBeVisible();
  await follower.reload();await expect(follower.getByRole('button',{name:'Unfollow',exact:true})).toBeVisible();
  await follower.goto('/feed');await follower.getByRole('button',{name:'Following posts',exact:true}).click();
  await expect(follower.getByRole('heading',{name:'Following feed',exact:true})).toBeVisible();
  await expect(follower.getByText(publicPost,{exact:true})).toBeVisible();
  await expect(follower.getByText(privatePost,{exact:true})).toHaveCount(0);
  await follower.getByRole('button',{name:'Recent posts',exact:true}).click();
  await follower.getByRole('button',{name:'Following posts',exact:true}).click();
  await expect(follower.getByText(publicPost,{exact:true})).toBeVisible();
  expect((await new AxeBuilder({page:follower}).analyze()).violations).toEqual([]);

  await follower.goto('http://localhost:3100');
  const list=follower.getByRole('region',{name:'Following',exact:true});
  await expect(list.getByText('Following: 1',{exact:true})).toBeVisible();
  await expect(list.getByRole('link',{name:'Followee Member',exact:true})).toBeVisible();
  await expect(follower.getByRole('region',{name:'My network',exact:true}).getByText('Connections: 0',{exact:true})).toBeVisible();
  await page.reload();await expect(page.getByRole('region',{name:'Following',exact:true}).getByText('Following: 0',{exact:true})).toBeVisible();
  await visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true}).click();await expect(visibility.getByText('Visibility saved.',{exact:true})).toBeVisible();
  await expect(visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true})).not.toBeChecked();
  await list.getByRole('button',{name:'Reload following',exact:true}).click();
  await expect(list.getByRole('heading',{name:'Private member',exact:true})).toBeVisible();
  await expect(list.getByRole('link',{name:'Followee Member',exact:true})).toHaveCount(0);
  await list.getByRole('button',{name:'Unfollow Private member',exact:true}).click();
  await expect(list.getByText('Following: 0',{exact:true})).toBeVisible();
  await follower.goto('/feed');await follower.getByRole('button',{name:'Following posts',exact:true}).click();
  await expect(follower.getByRole('heading',{name:'No posts from people you follow',exact:true})).toBeVisible();
  await expect(follower.getByText(publicPost,{exact:true})).toHaveCount(0);
  await visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true}).click();await expect(visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true})).toBeChecked();
  await expect(visibility.getByRole('checkbox',{name:'Visible to signed-in members',exact:true})).toBeEnabled();
  await follower.goto('http://localhost:3100'+path);
  await follow.click();await expect(follower.getByRole('button',{name:'Unfollow',exact:true})).toBeVisible();
  follower.once('dialog',dialog=>dialog.accept());
  await follower.getByRole('button',{name:'Block member',exact:true}).click();
  await expect(follower.getByRole('button',{name:'Unblock Followee Member',exact:true})).toBeVisible();
  await expect(list.getByText('Following: 0',{exact:true})).toBeVisible();
  follower.once('dialog',dialog=>dialog.accept());
  await follower.getByRole('button',{name:'Unblock Followee Member',exact:true}).click();
  await expect(follower.getByRole('button',{name:'Unblock Followee Member',exact:true})).toHaveCount(0);
  await follower.goto('http://localhost:3100'+path);
  await expect(follow).toBeEnabled();
  await expect(follower.getByRole('button',{name:'Unfollow',exact:true})).toHaveCount(0);
  expect((await new AxeBuilder({page:follower}).withTags(['wcag2a','wcag2aa','wcag21a','wcag21aa']).analyze()).violations).toEqual([]);
 }finally{await context.close()}
});
