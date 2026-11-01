import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
test('threaded replies persist across users and disappear when the parent is removed',async({page,request,browser})=>{
 await register(page,request,'ThreadOwner');const text='Threaded discussion '+Date.now();
 await page.goto('/feed');await page.getByRole('button',{name:'Start a post',exact:true}).click();await page.getByRole('textbox',{name:'Post text',exact:true}).fill(text);await page.getByRole('combobox',{name:'Post visibility'}).selectOption('MEMBERS');await page.getByRole('button',{name:'Publish post',exact:true}).click();await expect(page.getByRole('dialog')).not.toBeVisible();
 const post=page.getByRole('article',{name:'Post by ThreadOwner Member'}).filter({hasText:text});await post.locator('summary').filter({hasText:/^Comments$/}).click();await post.getByRole('textbox',{name:'Write a comment',exact:true}).fill('A discussion starter');await post.getByRole('button',{name:'Post comment',exact:true}).click();await expect(post.getByText('Comment posted.',{exact:true})).toBeVisible();
 const context=await browser.newContext();try{
  const reader=await context.newPage();await register(reader,request,'ThreadReader');await reader.goto('/feed');const other=reader.getByRole('article',{name:'Post by ThreadOwner Member'}).filter({hasText:text});
  await other.locator('summary').filter({hasText:/^Comments$/}).click();await other.locator('summary').filter({hasText:/^Replies$/}).click();await other.getByRole('textbox',{name:'Write a reply',exact:true}).fill('A persisted reply');await other.getByRole('button',{name:'Post reply',exact:true}).click();await expect(other.getByText('Reply posted.',{exact:true})).toBeVisible();
  await reader.reload();await other.locator('summary').filter({hasText:/^Comments$/}).click();await other.locator('summary').filter({hasText:/^Replies$/}).click();await expect(other.getByText('A persisted reply',{exact:true})).toBeVisible();
  await post.locator('summary').filter({hasText:/^Replies$/}).click();await expect(post.getByText('A persisted reply',{exact:true})).toBeVisible();
  await reader.setViewportSize({width:390,height:844});await reader.emulateMedia({colorScheme:'dark'});expect(await reader.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);expect((await new AxeBuilder({page:reader}).analyze()).violations).toEqual([]);
  page.once('dialog',d=>d.accept());const parent=post.getByRole('article',{name:'Comment by ThreadOwner Member',exact:true});await parent.getByRole('button',{name:'Delete comment',exact:true}).click();await expect(post.getByText('Comment deleted.',{exact:true})).toBeVisible();
  await other.getByRole('button',{name:'Refresh replies',exact:true}).click();await expect(other.getByText('A persisted reply',{exact:true})).toHaveCount(0);await expect(other.getByText('This post or parent comment is no longer available. Refresh the feed.',{exact:true})).toBeVisible();
 }finally{await context.close()}
});
