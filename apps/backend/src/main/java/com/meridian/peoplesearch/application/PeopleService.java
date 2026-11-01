package com.meridian.peoplesearch.application;
import com.meridian.peoplesearch.domain.People.*;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class PeopleService implements PeopleUseCases {
 private final PeopleStore store;private final Transactions transactions;
 public PeopleService(PeopleStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public Discovery discovery(String subject){return store.discovery(subject).orElseThrow(()->new UseCaseFailure(CONFLICT,"Initialize profile first"));}
 public Discovery discovery(String subject,boolean enabled,long version){
  if(version<0)throw new UseCaseFailure(INVALID,"Invalid revision");
  return transactions.write(()->{if(!store.discovery(subject,enabled,version))throw new UseCaseFailure(CONFLICT,"Discovery preference changed. Reload before retrying.");return discovery(subject);});
 }
 public Page search(String subject,String query,String location,UUID after){
  final Criteria criteria;try{criteria=new Criteria(query,location);}catch(IllegalArgumentException invalid){throw new UseCaseFailure(INVALID,invalid.getMessage());}
  var rows=store.search(subject,criteria,after);return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).memberId().toString():null);
 }
}
