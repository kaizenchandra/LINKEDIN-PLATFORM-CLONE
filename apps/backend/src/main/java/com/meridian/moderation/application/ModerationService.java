package com.meridian.moderation.application;
import com.meridian.moderation.domain.Moderation.*;
import java.util.*;
import com.meridian.content.application.ContentAccess;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class ModerationService implements ModerationUseCases {
 private final ModerationStore store;private final ContentAccess content;private final Transactions transactions;
 public ModerationService(ModerationStore store,ContentAccess content,Transactions transactions){this.store=store;this.content=content;this.transactions=transactions;}
 private UseCaseFailure conflict(){return new UseCaseFailure(CONFLICT,"Report already exists, changed or unavailable");}
 private void operator(String subject,boolean lock){if(!store.operator(subject,lock))throw new UseCaseFailure(FORBIDDEN,"Moderator access required");}
 public Access access(String subject){return new Access(store.operator(subject,false));}
 public Page reports(String subject,UUID after){var rows=store.reports(subject,after);return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null);}
 public Report submit(String subject,UUID id,UUID postId,Category category,String details){
  if(postId==null||category==null||details==null||details.length()>2000)throw new UseCaseFailure(INVALID,"Invalid report");String text=details.strip();
  return transactions.write(()->{
   var same=store.matching(id,subject,postId,category,text);if(same.isPresent())return same.get();
   if(!content.lockVisible(subject,postId))throw new UseCaseFailure(NOT_FOUND,"Post unavailable");
   if(store.authored(subject,postId)||!store.lockProfile(subject))throw conflict();
   if(store.dailyCount(subject)>=20)throw new UseCaseFailure(RATE_LIMITED,"Daily reporting limit reached");
   if(!store.insert(id,subject,postId,category,text))return store.matching(id,subject,postId,category,text).orElseThrow(this::conflict);
   return store.report(id);
  });
 }
 public ReviewQueue queue(String subject,UUID after){operator(subject,false);var rows=store.queue(after);return new ReviewQueue(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).report().id().toString():null);}
 public Report decide(String subject,UUID id,Decision decision,String reason,long version,long postVersion){
  if(decision==null||reason==null||reason.isBlank()||reason.length()>1000||version<1||postVersion<1)throw new UseCaseFailure(INVALID,"Invalid review");String text=reason.strip();
  return transactions.write(()->{
   operator(subject,true);var target=store.lockReport(id).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Report unavailable"));
   if(target.selfReview(subject))throw new UseCaseFailure(FORBIDDEN,"Another moderator must review this report");
   if(!"OPEN".equals(target.state())){
    if(target.matchesReplay(decision,version)&&store.auditMatches(id,subject,decision,text,postVersion))return store.report(id);throw conflict();
   }
   if(target.version()!=version||target.postVersion()!=postVersion)throw conflict();
   if(decision==Decision.REMOVED)store.removePost(target.postId());
   store.decide(id,decision);store.audit(id,subject,decision,text,postVersion);return store.report(id);
  });
 }
}
