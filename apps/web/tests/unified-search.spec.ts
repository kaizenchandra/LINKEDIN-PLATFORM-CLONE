import {test,expect} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
test('search tabs retain URLs and show only authorized persisted posts',async({page,request,browser})=>{
 await register(page,request,'SearchAuthor');const text='SearchNeedle'+Date.now();
 await page.goto('/feed');await page.getByRole('button',{name:'Start a post',exact:true}).click();
 await page.getByRole('textbox',{name:'Post text',exact:true}).fill(text);
 await page.getByRole('button',{name:'Publish post',exact:true}).click();await expect(page.getByRole('dialog')).not.toBeVisible();
 await page.goto('/search?type=posts&q='+text);await expect(page.getByText(text,{exact:true})).toBeVisible();
 await page.reload();await expect(page.getByText(text,{exact:true})).toBeVisible();
 const categories=page.getByRole('navigation',{name:'Search categories'});
 for(const label of ['Companies','Jobs','People']){await categories.getByRole('button',{name:label,exact:true}).click();await expect(page).toHaveURL(new RegExp('type='+label.toLowerCase()));await expect(page.getByRole('heading',{name:'Find '+label.toLowerCase(),exact:true})).toBeVisible();}
 await page.goBack();await expect(page.getByRole('heading',{name:'Find jobs',exact:true})).toBeVisible();
 await categories.getByRole('button',{name:'Posts',exact:true}).click();await expect(page.getByText(text,{exact:true})).toBeVisible();
 const context=await browser.newContext();try{
  const reader=await context.newPage();await register(reader,request,'SearchReader');await reader.goto('/search?type=posts&q='+text);
  await expect(reader.getByRole('status').first()).toHaveText('No matching posts. Try another search.');
  await page.goto('/feed');const own=page.getByRole('article',{name:'Post by SearchAuthor Member'}).filter({hasText:text});
  await own.getByRole('button',{name:'Edit post',exact:true}).click();await page.getByRole('combobox',{name:'Post visibility'}).selectOption('MEMBERS');
  await page.getByRole('button',{name:'Publish post',exact:true}).click();await expect(page.getByRole('dialog')).not.toBeVisible();
  await reader.getByRole('button',{name:'Refresh results',exact:true}).click();await expect(reader.getByText(text,{exact:true})).toBeVisible();
  expect((await new AxeBuilder({page:reader}).analyze()).violations).toEqual([]);
  await reader.setViewportSize({width:390,height:844});await reader.emulateMedia({colorScheme:'dark'});
  expect(await reader.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  expect((await new AxeBuilder({page:reader}).analyze()).violations).toEqual([]);
  page.once('dialog',dialog=>dialog.accept());await own.getByRole('button',{name:'Delete post',exact:true}).click();await expect(own).toHaveCount(0);
  await reader.getByRole('button',{name:'Refresh results',exact:true}).click();await expect(reader.getByRole('status').first()).toHaveText('No matching posts. Try another search.');
 }finally{await context.close()}
});
