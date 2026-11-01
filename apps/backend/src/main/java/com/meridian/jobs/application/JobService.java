package com.meridian.jobs.application;
import com.meridian.jobs.domain.Jobs.*;
import com.meridian.jobs.domain.Workplace;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class JobService implements JobUseCases {
 private final JobStore store;private final Transactions transactions;
 public JobService(JobStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public Page list(String subject,String query,String location,View view,UUID organization,Workplace workplace,UUID after){
  query=query.strip();location=location.strip();if(query.length()>80||location.length()>120)throw new UseCaseFailure(INVALID,"Invalid search length");
  var rows=store.list(subject,query,location,view,organization,workplace,after);return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null);
 }
 public Job get(String subject,UUID id){return store.get(subject,id).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Job unavailable"));}
 public Job publish(String subject,UUID id,Write input){
  final Write value;try{value=input.normalized();}catch(IllegalArgumentException invalid){throw new UseCaseFailure(INVALID,invalid.getMessage());}
  return transactions.write(()->{
   if(!store.lockOrganization(value.organizationId()))throw new UseCaseFailure(NOT_FOUND,"Company unavailable");
   if(!store.recruitingAllowed(subject,value.organizationId()))throw new UseCaseFailure(FORBIDDEN,"Recruiting access required");
   if(value.version()==0){if(value.status()!=Status.OPEN)throw new UseCaseFailure(INVALID,"New jobs must be open");if(store.insert(id,value))return get(subject,id);}
   var organization=store.lockJobOrganization(id);if(organization.isEmpty()||!organization.get().equals(value.organizationId()))throw new UseCaseFailure(CONFLICT,"Job or company mismatch");
   if(value.version()==0){if(value.matchesCreation(get(subject,id)))return get(subject,id);throw new UseCaseFailure(CONFLICT,"Job creation already completed");}
   if(!store.update(id,value))throw new UseCaseFailure(CONFLICT,"Job changed; reload before retrying");return get(subject,id);
  });
 }
 public void marker(String subject,UUID id,Marker marker,boolean enabled){transactions.write(()->{
  if(enabled){if(!store.initialized(subject))throw new UseCaseFailure(CONFLICT,"Initialize profile first");get(subject,id);}
  store.marker(subject,id,marker,enabled);return null;
 });}
}
