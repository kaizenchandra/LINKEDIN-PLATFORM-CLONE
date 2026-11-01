package com.meridian.exports.application;
import java.util.*;
import java.time.Clock;
import com.meridian.exports.domain.ExportRecord;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class ExportLifecycleService implements ExportLifecycleUseCases {
 private final ExportLifecycleStore store;private final Transactions transactions;private final Clock clock;
 public ExportLifecycleService(ExportLifecycleStore store,Transactions transactions,Clock clock){this.store=store;this.transactions=transactions;this.clock=clock;}
 private Optional<ExportRecord> owned(String subject,UUID id){return store.owned(subject,id).map(e->e.at(clock.instant()));}
 private UseCaseFailure unavailable(){return new UseCaseFailure(NOT_FOUND,"Export unavailable");}
 public List<ExportRecord> list(String subject){return store.list(subject).stream().map(e->e.at(clock.instant())).toList();}
 public Requested request(String subject,UUID id){return transactions.write(()->{
  if(!store.lockProfile(subject))throw new UseCaseFailure(CONFLICT,"Initialize your profile first");
  var old=owned(subject,id);if(old.isPresent())return new Requested(old.get(),false);
  if(store.pending(subject))throw new UseCaseFailure(CONFLICT,"An export is already being prepared");
  if(store.recent(subject))return new Requested(null,true);
  if(!store.insert(subject,id))throw unavailable();store.audit(id,"REQUESTED");return new Requested(owned(subject,id).orElseThrow(this::unavailable),false);
 });}
 public byte[] download(String subject,UUID id){return transactions.write(()->{
  var export=owned(subject,id).orElseThrow(this::unavailable);
  if(export.status().equals("EXPIRED"))throw new UseCaseFailure(GONE,"Export expired");
  if(!export.status().equals("READY"))throw new UseCaseFailure(CONFLICT,"Export is not ready");
  var bytes=store.lockPayload(subject,id).orElseThrow(()->new UseCaseFailure(GONE,"Export expired"));store.audit(id,"DOWNLOADED");return bytes;
 });}
 public void cancel(String subject,UUID id){transactions.write(()->{owned(subject,id).orElseThrow(this::unavailable);if(store.cancel(subject,id))store.audit(id,"CANCELED");return null;});}
}
