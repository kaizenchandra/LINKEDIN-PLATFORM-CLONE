import {test,expect} from '@playwright/test';
import {register} from './helpers/register';
test.use({trace:'off'});
test('public profiles allow guests and private links require login with return',async({page,request,browser})=>{
 const email=await register(page,request,'PublicOwner');
 const settings=page.getByRole('region',{name:'Profile visibility',exact:true});
 const path=(await settings.getByRole('link',{name:'View member profile'}).getAttribute('href'))!;
 const publicToggle=settings.getByRole('checkbox',{name:'Public profile — visible without signing in'});
 await expect(publicToggle).not.toBeChecked();
 await publicToggle.click();await expect(settings.getByText('Visibility saved.',{exact:true})).toBeVisible();
 const context=await browser.newContext();try{
 const guest=await context.newPage();await guest.goto(path);
 await expect(guest.getByRole('heading',{name:'PublicOwner Member',exact:true})).toBeVisible();
 await expect(guest.getByRole('button',{name:'Connect',exact:true})).toHaveCount(0);
 await publicToggle.click();await expect(publicToggle).not.toBeChecked();await expect(settings.getByText('Visibility saved.',{exact:true})).toBeVisible();
 await guest.reload();await expect(guest).toHaveURL(/localhost:28080/);
 await guest.locator('input[name="username"]').fill(email);
 await guest.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
 await guest.getByRole('button',{name:'Sign In',exact:true}).click();
 await expect(guest).toHaveURL(`http://localhost:3100${path}`);
 await expect(guest.getByRole('heading',{name:'PublicOwner Member',exact:true})).toBeVisible();
 }finally{await context.close()}
});
