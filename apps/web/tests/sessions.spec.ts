import {test,expect,Page} from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import {register} from './helpers/register';
test.use({trace:'off'});
async function login(page:Page,email:string){
 await page.goto('/');await page.getByRole('link',{name:'Sign in / Register',exact:true}).click();
 await expect(page.locator('input[name="password"]')).toBeVisible();
 const emailField=page.getByRole('textbox',{name:'Email',exact:true});if(await emailField.isVisible())await emailField.fill(email);
 await page.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
 await page.getByRole('button',{name:'Sign In',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Sessions Member',exact:true})).toBeVisible();
}
test('settings persist preferences and revoke individual and all real browser sessions',async({page,request,browser},testInfo)=>{
 const email=await register(page,request,'Sessions');
 const secondContext=await browser.newContext();try{
 const second=await secondContext.newPage();await login(second,email);
 await second.goto('/notifications');await expect(second.getByText('Live updates connected.',{exact:true})).toBeVisible();
 await page.locator('.account-menu summary').click();await page.getByRole('link',{name:'Settings & privacy',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Settings',exact:true})).toBeVisible();
 const sessions=page.getByRole('region',{name:'Where you are signed in',exact:true});
 await expect(sessions.getByRole('listitem')).toHaveCount(2);
 const rows=await (await page.request.get('/api/v1/me/sessions')).json();
 expect(rows.filter((row:{current:boolean})=>row.current)).toHaveLength(1);
 const cookies=await page.context().cookies();
 for(const row of rows){expect(Object.keys(row).sort()).toEqual(['createdAt','current','expiresAt','id','lastActiveAt']);expect(cookies.some(cookie=>cookie.value===row.id)).toBeFalsy();}
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.screenshot({path:testInfo.outputPath('settings-security-desktop.png'),fullPage:true});
 await page.getByRole('link',{name:'Account preferences',exact:true}).click();
 await page.getByRole('combobox',{name:'Appearance',exact:true}).selectOption('dark');await expect(page.getByText('Appearance saved to your account.',{exact:true})).toBeVisible();
 await page.reload();await expect(page.getByRole('combobox',{name:'Appearance',exact:true})).toHaveValue('dark');
 await page.getByRole('link',{name:'Sign in & security',exact:true}).click();await expect(sessions.getByRole('listitem')).toHaveCount(2);
 await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();
 expect((await new AxeBuilder({page}).analyze()).violations).toEqual([]);
 await page.screenshot({path:testInfo.outputPath('settings-security-mobile-dark.png'),fullPage:true});
 page.once('dialog',dialog=>dialog.accept());await sessions.getByRole('button',{name:/Sign out other session/}).click();
 await expect(sessions.getByText('Session signed out.',{exact:true})).toBeVisible();await expect(sessions.getByRole('listitem')).toHaveCount(1);
 expect((await second.request.get('/api/v1/me')).status()).toBe(401);
 await expect(second.getByText('Live updates connected.',{exact:true})).toHaveCount(0,{timeout:10000});
 // Existing Keycloak SSO cookies must not silently recreate the revoked app session.
 await login(second,email);
 await sessions.getByRole('button',{name:'Refresh sessions',exact:true}).click();await expect(sessions.getByRole('listitem')).toHaveCount(2);
 page.once('dialog',dialog=>dialog.accept());await sessions.getByRole('button',{name:'Sign out of Meridian on all devices',exact:true}).click();
 await expect(page.getByText('Your Meridian session has ended. Sign in to manage settings.',{exact:true})).toBeVisible();
 expect((await page.request.get('/api/v1/me')).status()).toBe(401);expect((await second.request.get('/api/v1/me')).status()).toBe(401);
 await page.reload();await expect(page.getByRole('button',{name:'Refresh sessions',exact:true})).toHaveCount(0);
 }finally{await secondContext.close()}
});
