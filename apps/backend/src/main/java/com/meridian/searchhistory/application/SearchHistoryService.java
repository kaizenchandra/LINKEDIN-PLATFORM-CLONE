package com.meridian.searchhistory.application;
import com.meridian.searchhistory.domain.SearchHistory.*;
import com.meridian.shared.application.*;
import java.util.UUID;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class SearchHistoryService implements SearchHistoryUseCases {
 private final SearchHistoryStore store;private final Transactions transactions;
 public SearchHistoryService(SearchHistoryStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public State get(String subject){return transactions.snapshot(()->store.state(subject));}
 private State lock(String subject,long version){
  if(version<0)throw new UseCaseFailure(INVALID,"Invalid revision");
  if(!store.lockOwner(subject))throw conflict();
  store.initialize(subject);State current=store.state(subject);
  if(current.version()!=version)throw conflict();
  store.purgeOwnerExpired(subject);return current;
 }
 public State preference(String subject,boolean enabled,long version){return transactions.write(()->{
  lock(subject,version);store.preference(subject,enabled);if(!enabled)store.clear(subject);return store.state(subject);
 });}
 public State remember(String subject,Kind kind,String query,String location,long version){
  final Criteria criteria;
  try{criteria=new Criteria(kind,query,location);}catch(IllegalArgumentException failure){throw new UseCaseFailure(INVALID,failure.getMessage());}
  return transactions.write(()->{if(!lock(subject,version).enabled())throw conflict();store.remember(subject,criteria);return store.state(subject);});
 }
 public State clear(String subject,long version){return transactions.write(()->{
  lock(subject,version);store.clear(subject);store.advance(subject);return store.state(subject);
 });}
 public State remove(String subject,UUID id,long version){return transactions.write(()->{
  lock(subject,version);store.remove(subject,id);store.advance(subject);return store.state(subject);
 });}
 public int purgeExpired(){return transactions.write(store::purgeExpired);}
 private UseCaseFailure conflict(){return new UseCaseFailure(CONFLICT,"Search history preference changed or saving is disabled. Refresh before retrying.");}
}
