import assert from 'node:assert/strict';
const origin='http://localhost:19090';
async function query(expression){
 const response=await fetch(`${origin}/api/v1/query?query=${encodeURIComponent(expression)}`,{signal:AbortSignal.timeout(3000)});
 assert.equal(response.status,200);const body=await response.json();assert.equal(body.status,'success');return body.data.result;
}
for(const path of ['/readyz','/livez']){
 const response=await fetch(`http://localhost:8081${path}`);assert.equal(response.status,200);assert.deepEqual(await response.json(),{status:'UP'});
}
const blocked=await fetch('http://localhost:8081/actuator/prometheus',{redirect:'manual'});assert.equal(blocked.status,401);
// Produce a real HTTP observation; no identity or request payload enters metric labels.
assert.equal((await fetch('http://localhost:8081/api/v1/me')).status,401);
let verified=false;
for(let attempt=0;attempt<30;attempt++){
 const up=await query('up{job="meridian-backend"}');
 const http=await query('http_server_requests_seconds_count{job="meridian-backend"}');
 const jvm=await query('jvm_memory_used_bytes{job="meridian-backend"}');
 if(up.length===1&&up[0].value[1]==='1'&&http.length>0&&jvm.length>0){verified=true;break;}
 await new Promise(resolve=>setTimeout(resolve,1000));
}
assert.ok(verified,'Prometheus did not ingest backend health, HTTP and JVM metrics');
console.log('Readiness, liveness, public metrics denial and real Prometheus ingestion verified.');
