import {test,expect,Page} from '@playwright/test';
import {register} from './helpers/register';
test.use({trace:'off',screenshot:'off',video:'off'});
async function logout(page:Page){
 await page.goto('/');await page.locator('.account-menu summary').click();
 await page.getByRole('button',{name:'Sign out',exact:true}).click();await expect(page).toHaveURL(/28080.*logout/);
}
async function password(page:Page,email:string){
 await page.goto('/');await page.getByRole('link',{name:'Sign in / Register',exact:true}).click();
 await page.getByRole('textbox',{name:'Email',exact:true}).fill(email);
 await page.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
 await page.getByRole('button',{name:'Sign In',exact:true}).click();
 await expect(page.locator('#recoveryCodeInput')).toBeVisible();
}
test('native recovery codes enroll, require a password and reject reuse',async({page,request})=>{
 const email=await register(page,request,'Recovery');
 try{
  await page.goto('/settings');await page.getByRole('link',{name:'Passkeys and recovery codes'}).click();
  await page.getByRole('button',{name:'Set up Recovery authentication codes',exact:true}).click();
  await expect(page.locator('#kc-recovery-codes-list')).toBeVisible();
  // Secrets stay only in test memory: no traces, screenshots, downloads or logs.
  const codes=await page.locator('#kc-recovery-codes-list li').allTextContents();
  const first=codes[0].replace(/^\s*1:\s*/, '').trim();
  const second=codes[1].replace(/^\s*2:\s*/, '').trim();
  expect(codes.length).toBe(12);
  await expect(page.locator('#saveRecoveryAuthnCodesBtn')).toBeDisabled();
  await page.locator('#kcRecoveryCodesConfirmationCheck').check();await page.locator('#saveRecoveryAuthnCodesBtn').click();
  await expect(page.getByRole('heading',{name:'Signing in',exact:true})).toBeVisible();
  await logout(page);await password(page,email);
  expect((await page.request.get('http://localhost:3100/api/v1/me')).status()).toBe(401);
  await page.locator('#recoveryCodeInput').fill(first);await page.getByRole('button',{name:'Sign In',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Recovery Member',exact:true})).toBeVisible();
  await logout(page);await password(page,email);
  await page.locator('#recoveryCodeInput').fill(first);await page.getByRole('button',{name:'Sign In',exact:true}).click();
  await expect(page.locator('#recoveryCodeInput')).toBeVisible();
  expect((await page.request.get('http://localhost:3100/api/v1/me')).status()).toBe(401);
  await page.locator('#recoveryCodeInput').fill(second);await page.getByRole('button',{name:'Sign In',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Recovery Member',exact:true})).toBeVisible();
 }finally{await page.goto('about:blank');}
});
