package com.meridian.events.application;
import com.meridian.shared.application.Transactions;
public final class OutboxService implements OutboxUseCases {
 private final OutboxStore store;private final OutboxPublisher publisher;private final OutboxMetrics metrics;private final Transactions transactions;
 public OutboxService(OutboxStore store,OutboxPublisher publisher,OutboxMetrics metrics,Transactions transactions){this.store=store;this.publisher=publisher;this.metrics=metrics;this.transactions=transactions;}
 public void publishReady(){transactions.write(()->{
  for(var event:store.lockReady()){
   try{publisher.publish(event);store.published(event.id());metrics.published();}
   catch(Exception failure){store.retry(event.id());metrics.retry();break;}
  }
  return null;
 });}
}
