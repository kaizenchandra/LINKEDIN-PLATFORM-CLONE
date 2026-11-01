package com.meridian.messaging.application;
import java.util.*;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class MessagingWriteService implements MessagingWriteUseCases {
 private final MessagingWriteStore store;private final MessagingSignals signals;private final Transactions transactions;
 public MessagingWriteService(MessagingWriteStore store,MessagingSignals signals,Transactions transactions){this.store=store;this.signals=signals;this.transactions=transactions;}
 private UseCaseFailure unavailable(){return new UseCaseFailure(NOT_FOUND,"Conversation or connection unavailable");}
 public UUID start(String subject,UUID member){
  if(member==null)throw new UseCaseFailure(INVALID,"Member required");
  return transactions.write(()->{String target=store.target(member,subject).orElseThrow(this::unavailable);store.lockPair(subject,target);if(!store.connected(subject,target)||store.blocked(subject,target))throw unavailable();UUID id=store.start(subject,target);signals.changed(Set.of(subject,target));return id;});
 }
 public Message send(String subject,UUID conversation,UUID message,String text){
  if(text==null||text.isBlank()||text.length()>3000)throw new UseCaseFailure(INVALID,"Message must not be blank");
  return transactions.write(()->{
   var access=store.access(conversation,subject,true);if(!store.connected(access.a(),access.b()))throw unavailable();String body=text.strip();if(body.isEmpty())throw new UseCaseFailure(INVALID,"Message must not be blank");
   var previous=store.previous(message);
   if(previous.isPresent()){if(!previous.get().matches(conversation,subject,body))throw new UseCaseFailure(CONFLICT,"Message ID already used");}
   else{
    if(!store.insert(message,conversation,access.latest()+1,subject,body))throw new UseCaseFailure(CONFLICT,"Message ID already used");
    signals.changed(Set.of(access.a(),access.b()));store.advance(conversation);
   }
   return store.receipt(message,subject,access);
  });
 }
 public Preference preferences(String subject,UUID conversation,Preference desired){
  if(desired==null||desired.starred()==null||desired.archived()==null||desired.version()==null||desired.version()<0)throw new UseCaseFailure(INVALID,"Invalid conversation preferences");
  return transactions.write(()->{var access=store.access(conversation,subject,true);if(!store.preferences(conversation,subject.equals(access.a()),desired))throw new UseCaseFailure(CONFLICT,"Conversation settings changed. Refresh before retrying.");signals.changed(Set.of(subject));return store.access(conversation,subject,false).preferences(subject);});
 }
 public void read(String subject,UUID conversation,long through){
  if(through<0)throw new UseCaseFailure(INVALID,"Invalid read cursor");
  transactions.write(()->{var access=store.access(conversation,subject,true);if(through>access.latest())throw new UseCaseFailure(INVALID,"Read cursor exceeds conversation");store.read(conversation,subject.equals(access.a()),through);signals.changed(Set.of(access.a(),access.b()));return null;});
 }
}
