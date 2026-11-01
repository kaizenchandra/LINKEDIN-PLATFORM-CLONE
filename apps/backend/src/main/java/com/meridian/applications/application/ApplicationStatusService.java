package com.meridian.applications.application;
import java.util.*;
import com.meridian.applications.domain.Applications.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class ApplicationStatusService implements ApplicationStatusUseCases {
 private final ApplicationStatusStore store;private final ApplicationSignals signals;private final Transactions transactions;
 public ApplicationStatusService(ApplicationStatusStore store,ApplicationSignals signals,Transactions transactions){this.store=store;this.signals=signals;this.transactions=transactions;}
 public Application change(String subject,UUID id,State target,long version){
  if(target==null||version<1)throw new UseCaseFailure(INVALID,"Invalid application change");
  return transactions.write(()->{
   UUID organization=store.organization(id,subject).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Application unavailable"));
   if(!store.lockOrganization(organization))throw new UseCaseFailure(NOT_FOUND,"Company unavailable");
   var current=store.lockStatus(id,subject).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Application unavailable"));
   if(!current.actorAllowed(subject,target))throw new UseCaseFailure(FORBIDDEN,"Actor cannot perform this transition");
   if(version!=current.version()||!current.canTransition(subject,target))throw new UseCaseFailure(CONFLICT,"Application changed or transition is unavailable");
   store.update(id,target);store.auditAndOutbox(id,version+1,subject,current.state(),target);
   signals.changed(store.audience(organization,current.applicant()));return store.receipt(id,subject);
  });
 }
}
