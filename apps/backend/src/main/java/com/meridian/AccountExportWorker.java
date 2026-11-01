package com.meridian;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

@Component @EnableScheduling
@ConditionalOnProperty(name="meridian.exports.worker-enabled",havingValue="true",matchIfMissing=true)
class AccountExportWorker {
 private final AccountExportQueue queue;private final AccountDataSnapshot snapshots;private final MeterRegistry metrics;
 AccountExportWorker(AccountExportQueue queue,AccountDataSnapshot snapshots,MeterRegistry metrics){this.queue=queue;this.snapshots=snapshots;this.metrics=metrics;}
 @Scheduled(fixedDelay=5000) public void tick(){
  AccountExportQueue.Work work=null;
  try{
   work=queue.claim();if(work==null)return;
   if(queue.complete(work,snapshots.generate(work.subject())))metrics.counter("meridian.exports.completed").increment();
  }catch(RuntimeException failure){
   metrics.counter("meridian.exports.failures").increment();
   if(work!=null)try{queue.fail(work,failure instanceof AccountDataSnapshot.TooLarge);}catch(RuntimeException unavailable){/* Lease recovery retries after storage recovers. */}
   // No SQL exception, snapshot body, identifiers or personal values are logged.
  }
 }
}
