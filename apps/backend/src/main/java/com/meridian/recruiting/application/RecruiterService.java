package com.meridian.recruiting.application;
import java.util.*;
import com.meridian.recruiting.domain.RecruiterAccess.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class RecruiterService implements RecruiterUseCases {
 private final RecruiterStore store;private final Transactions transactions;private final RecruiterSignals signals;
 public RecruiterService(RecruiterStore store,Transactions transactions,RecruiterSignals signals){this.store=store;this.transactions=transactions;this.signals=signals;}
 public Page list(String subject,UUID organization,UUID after){
  if(organization!=null&&!store.owns(subject,organization))throw new UseCaseFailure(FORBIDDEN,"Company owner required");
  var rows=store.list(subject,organization,after);return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null);
 }
 private String owner(UUID organization){return store.lockOrganization(organization).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Company unavailable"));}
 public Access invite(String subject,UUID organization,UUID id,UUID connection){
  if(connection==null)throw new UseCaseFailure(INVALID,"Connection required");
  return transactions.write(()->{
   if(!owner(organization).equals(subject))throw new UseCaseFailure(FORBIDDEN,"Company owner required");
   var existing=store.invitation(id);
   if(existing.isPresent()){
    var old=existing.get();if(!organization.equals(old.organizationId())||!connection.equals(old.connectionId()))throw new UseCaseFailure(CONFLICT,"Invitation ID conflict");
    return store.receipt(id,subject);
   }
   String member=store.connectedMember(connection,subject).orElseThrow(()->new UseCaseFailure(CONFLICT,"Accepted connection required"));
   store.lockPair(subject,member);
   if(!store.acceptedUnblocked(connection,subject,member))throw new UseCaseFailure(CONFLICT,"Accepted unblocked connection required");
   if(store.occupied(organization)>=50)throw new UseCaseFailure(CONFLICT,"Recruiter capacity reached");
   if(!store.insert(id,organization,member,connection))throw new UseCaseFailure(CONFLICT,"Pending or active access already exists");
   store.audit(id,1,subject,null,"PENDING");signals.changed(Set.of(subject,member));return store.receipt(id,subject);
  });
 }
 public Access change(String subject,UUID id,Action action,long version){
  if(action==null||version<1)throw new UseCaseFailure(INVALID,"Invalid access change");
  return transactions.write(()->{
   UUID organization=store.accessibleOrganization(id,subject).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Access unavailable"));
   String owner=owner(organization);var membership=store.lockMembership(id);
   if(owner.equals(subject)!=(action==Action.REVOKE))throw new UseCaseFailure(FORBIDDEN,"Action not permitted");
   String next=membership.next(action);
   if(next==null||version!=membership.version())throw new UseCaseFailure(CONFLICT,"Access changed; reload before retrying");
   store.change(id,next);store.audit(id,version+1,subject,membership.state(),next);signals.changed(Set.of(owner,membership.member()));return store.receipt(id,subject);
  });
 }
}
