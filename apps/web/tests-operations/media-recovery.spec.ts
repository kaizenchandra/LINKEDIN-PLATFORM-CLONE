import {test,expect} from '@playwright/test';
import {execFileSync} from 'node:child_process';
import {resolve} from 'node:path';
import {register} from '../tests/helpers/register';
const root=resolve(process.cwd(),'../..');
const compose=['compose','--ansi','never','--progress','plain','--env-file','.env','-f','infrastructure/local/compose.yml','-f','infrastructure/local/compose.media.yml','--profile','app'];
function docker(args:string[]){return execFileSync('docker',[...compose,...args],{cwd:root,encoding:'utf8',timeout:60000});}
test('local photo survives loss of emulated S3 state and is restored without bypassing privacy',async({page,request})=>{
 await register(page,request,'MediaRecovery');
 const bytes=Buffer.from(await page.evaluate(()=>{const c=document.createElement('canvas');c.width=16;c.height=16;c.getContext('2d')!.fillRect(0,0,16,16);return c.toDataURL('image/png').split(',')[1]}),'base64');
 const csrf=await (await page.request.get('/api/v1/csrf')).json();
 expect((await page.request.put('/api/v1/me/photo',{headers:{'Content-Type':'image/png',[csrf.header]:csrf.token},data:bytes})).status()).toBe(204);
 const original=await page.request.get('/api/v1/me/photo');expect(original.status()).toBe(200);const normalized=await original.body();
 docker(['restart','localstack']);docker(['up','-d','--wait','--wait-timeout','120','localstack']);
 // Terraform recreates emulator infrastructure; it does not restore personal objects.
 execFileSync('terraform',['-chdir=infrastructure/terraform/localstack','apply','-input=false','-auto-approve'],{cwd:root,encoding:'utf8',timeout:120000});
 const restored=await page.request.get('/api/v1/me/photo');expect(restored.status()).toBe(200);expect(await restored.body()).toEqual(normalized);
 await page.reload();await expect(page.getByAltText("MediaRecovery Member's profile photo",{exact:true})).toBeVisible();
 const path=await page.getByRole('link',{name:'View member profile',exact:true}).getAttribute('href');
 expect((await request.get('/api/v1/profiles/'+path!.split('/').pop()+'/photo')).status()).toBe(401);
 // A second read must use the repaired object, which now physically exists in S3.
 const result=JSON.parse(docker(['exec','-T','localstack','awslocal','s3api','list-objects-v2','--bucket','meridian-local-media','--prefix','profiles/']));expect(result.Contents?.length??0).toBeGreaterThan(0);
 expect((await page.request.delete('/api/v1/me/photo',{headers:{[csrf.header]:csrf.token}})).status()).toBe(204);
 execFileSync('terraform',['-chdir=infrastructure/terraform/localstack','plan','-detailed-exitcode','-input=false'],{cwd:root,encoding:'utf8',timeout:60000});
});
