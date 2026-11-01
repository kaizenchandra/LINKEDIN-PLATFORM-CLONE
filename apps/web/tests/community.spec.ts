import {test,expect} from '@playwright/test';
import {register} from './helpers/register';
test('network communities persist joins, attendance and subscriptions',async({page,request})=>{
 test.setTimeout(120000);
 await register(page,request,'Community');
 await page.goto('/network');
 await page.getByRole('button',{name:'Catch up',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Catch up',exact:true})).toBeVisible();
 await expect(page.getByText('You’re all caught up. New posts from your connections will appear here.',{exact:true})).toBeVisible();
 for(const [tab,create,join,leave] of [['Groups','group','Join group','Leave group'],['Events','event','Attend event','Cancel attendance'],['Newsletters','newsletter','Subscribe','Unsubscribe']]){
  await page.getByRole('button',{name:tab,exact:true}).click();
  const title='Fictional '+create+' '+Date.now();
  await page.getByRole('button',{name:'Create '+create,exact:true}).click();
  await page.getByLabel('Title',{exact:true}).fill(title);
  await page.getByLabel('Description',{exact:true}).fill('A fictional professional community for automated testing.');
  if(tab==='Events'){
   await page.getByLabel('Starts at (your local time)',{exact:true}).fill('2030-01-01T10:00');
   await page.getByLabel('Location or meeting details',{exact:true}).fill('Online');
  }
  await page.getByRole('button',{name:'Create listing',exact:true}).click();
  const card=page.locator('article').filter({has:page.getByRole('heading',{name:title,exact:true})});
  await card.getByRole('button',{name:join,exact:true}).click();
  await expect(card.getByRole('button',{name:leave,exact:true})).toBeVisible();
  await page.reload();await expect(page.getByRole('heading',{name:tab,exact:true})).toBeVisible();
  await expect(card.getByRole('button',{name:leave,exact:true})).toBeVisible();
  await card.getByRole('button',{name:leave,exact:true}).click();
  await expect(card.getByRole('button',{name:join,exact:true})).toBeVisible();
 }
});
