package com.meridian.notifications.application;
import com.meridian.notifications.domain.Notifications.*;
import java.util.*;
import com.meridian.notifications.domain.Notifications;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class NotificationService implements NotificationUseCases {
 private final NotificationStore store;private final NotificationSignals signals;private final Transactions transactions;
 public NotificationService(NotificationStore store,NotificationSignals signals,Transactions transactions){this.store=store;this.signals=signals;this.transactions=transactions;}
 public Page list(String subject,Long before,Category category){
  if(before!=null&&before<1)throw new UseCaseFailure(INVALID,"Invalid cursor");
  var rows=store.list(subject,before,category);long unread=store.unreadCount(subject);Long through=store.readThrough(subject);
  return new Page(rows.stream().limit(20).map(Row::notification).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null,unread,through==null?null:through.toString());
 }
 public void change(String subject,UUID id,Change change){transactions.write(()->{
  if(!store.change(subject,id,change))throw new UseCaseFailure(NOT_FOUND,"Notification unavailable");signals.changed(subject);return null;
 });}
 public void readAll(String subject,String through){
  final long boundary;try{boundary=Notifications.boundary(through);}catch(IllegalArgumentException failure){throw new UseCaseFailure(INVALID,failure.getMessage());}
  transactions.write(()->{store.readAll(subject,boundary);signals.changed(subject);return null;});
 }
 public Preference preferences(String subject){return store.preferences(subject).orElse(new Preference(true,true,0));}
 public Preference preferences(String subject,boolean enabled,Boolean jobs,long version){
  if(version<0)throw new UseCaseFailure(INVALID,"Invalid revision");
  return transactions.write(()->{if(!store.preferences(subject,enabled,jobs,version))throw new UseCaseFailure(CONFLICT,"Preferences changed. Reload before retrying.");signals.changed(subject);return preferences(subject);});
 }
}
