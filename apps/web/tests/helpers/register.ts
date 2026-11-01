import {expect,Page,APIRequestContext} from '@playwright/test';
export async function register(page:Page,request:APIRequestContext,label:string,directJoin=false){
 const email=`visibility-${label.toLowerCase()}-${Date.now()}@example.test`;
 await page.goto('http://localhost:3100');
 if(directJoin){await page.getByRole('link',{name:'Join now',exact:true}).first().click();await expect(page).toHaveURL(/prompt=create/);}
 else{await page.getByRole('link',{name:'Sign in / Register'}).click();await page.getByRole('link',{name:'Register',exact:true}).click();}
 await page.getByRole('textbox',{name:'Email',exact:true}).fill(email);
 await page.getByRole('textbox',{name:'First name',exact:true}).fill(label);
 await page.getByRole('textbox',{name:'Last name',exact:true}).fill('Member');
 await page.locator('input[name="password"]').fill('Fictional-Test-Password-937!');
 await page.locator('input[name="password-confirm"]').fill('Fictional-Test-Password-937!');
 await page.getByRole('button',{name:'Register',exact:true}).click();
 let id='';await expect.poll(async()=>{const data=await (await request.get('http://localhost:8025/api/v1/messages')).json();id=data.messages?.find((m:{ID:string;To:{Address:string}[]})=>m.To.some(t=>t.Address===email))?.ID;return !!id},{timeout:15000}).toBe(true);
 const mail=await (await request.get(`http://localhost:8025/api/v1/message/${id}`)).json();
 await page.goto(mail.Text.match(/http:\/\/localhost:28080\/\S+/)[0]);
 await expect(page.getByRole('heading',{name:`${label} Member`,exact:true})).toBeVisible();
 return email;
}
