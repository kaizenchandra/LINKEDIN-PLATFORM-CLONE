package com.meridian.notifications.application;
import java.util.UUID;
import com.meridian.shared.application.Transactions;
public final class NotificationProjectionService implements NotificationProjectionUseCases {
 private final NotificationProjectionStore store;private final NotificationSignals signals;private final Transactions transactions;
 public NotificationProjectionService(NotificationProjectionStore store,NotificationSignals signals,Transactions transactions){this.store=store;this.signals=signals;this.transactions=transactions;}
 public void connectionAccepted(UUID event,UUID connection){
  if(event==null||connection==null)throw new IllegalArgumentException("Unsupported event envelope");
  transactions.write(()->{if(store.claim(event))store.insertConnection(event,connection).ifPresent(signals::changed);return null;});
 }
 public void applicationChanged(UUID event,UUID application,long version){
  if(event==null||application==null||version<1)throw new IllegalArgumentException("Unsupported event envelope");
  transactions.write(()->{
   var source=store.source(event,application,version);if(source.isEmpty()||!store.claim(event))return null;
   var row=source.get();String recipient=row.recipient();if(!store.jobsEnabled(recipient))return null;
   if(store.insertApplication(event,application,version,recipient,row.actor()))signals.changed(recipient);return null;
  });
 }
}
