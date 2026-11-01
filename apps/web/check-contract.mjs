import {readFileSync} from 'node:fs';
import {execFileSync} from 'node:child_process';
const path=new URL('./app/api-schema.d.ts',import.meta.url);
const previous=readFileSync(path,'utf8');
execFileSync('npm',['run','contracts:generate'],{stdio:'inherit'});
if(previous!==readFileSync(path,'utf8')){
 console.error('Generated API types were stale. Review and commit the regenerated file.');
 process.exit(1);
}
