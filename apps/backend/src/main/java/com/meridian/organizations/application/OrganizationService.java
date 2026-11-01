package com.meridian.organizations.application;
import com.meridian.organizations.domain.Organizations.*;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class OrganizationService implements OrganizationUseCases {
 private final OrganizationStore store;private final Transactions transactions;
 public OrganizationService(OrganizationStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 private void initialized(String subject){if(!store.initialized(subject))throw new UseCaseFailure(CONFLICT,"Initialize profile first");}
 public Page list(String subject,String query,View view,UUID after){
  query=query.strip();if(query.length()>80)throw new UseCaseFailure(INVALID,"Search is limited to 80 characters");
  var rows=store.list(subject,query,view,after);return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null);
 }
 public Organization get(String subject,UUID id){return store.get(subject,id).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Company unavailable"));}
 public Organization save(String subject,UUID id,Write input){
  final Write value;try{value=input.normalized();}catch(IllegalArgumentException invalid){throw new UseCaseFailure(INVALID,invalid.getMessage());}
  return transactions.write(()->{
   initialized(subject);
   if(value.version()==0&&store.insert(subject,id,value))return get(subject,id);
   String owner=store.lockOwner(id).orElseThrow(()->new UseCaseFailure(CONFLICT,"Company unavailable for update"));
   if(!owner.equals(subject))throw new UseCaseFailure(value.version()==0?CONFLICT:FORBIDDEN,"Only page owner may manage this company");
   if(value.version()==0){if(value.matchesCreation(get(subject,id)))return get(subject,id);throw new UseCaseFailure(CONFLICT,"Company creation already completed; reload before editing");}
   if(!store.update(id,value))throw new UseCaseFailure(CONFLICT,"Company changed. Reload before retrying.");return get(subject,id);
  });
 }
 public void follow(String subject,UUID id){transactions.write(()->{initialized(subject);get(subject,id);store.follow(subject,id);return null;});}
 public void unfollow(String subject,UUID id){transactions.write(()->{store.unfollow(subject,id);return null;});}
}
