package com.meridian.exports.application;
import com.meridian.exports.domain.ExportWork;
public final class ExportGenerationService implements ExportGenerationUseCases {
 private final ExportQueueUseCases queue;private final ExportSnapshot snapshots;private final ExportMetrics metrics;
 public ExportGenerationService(ExportQueueUseCases queue,ExportSnapshot snapshots,ExportMetrics metrics){this.queue=queue;this.snapshots=snapshots;this.metrics=metrics;}
 public void generateNext(){
  ExportWork work=null;
  try{work=queue.claim();if(work==null)return;if(queue.complete(work,snapshots.generate(work.subject())))metrics.completed();}
  catch(RuntimeException failure){metrics.failed();if(work!=null)try{queue.fail(work,failure instanceof ExportSnapshot.TooLarge);}catch(RuntimeException unavailable){/* Recovery proceeds after lease expiry. */}}
 }
}
