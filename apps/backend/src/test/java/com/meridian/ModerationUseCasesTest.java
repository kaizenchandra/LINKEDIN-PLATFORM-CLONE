package com.meridian;
import com.meridian.moderation.application.*;
import com.meridian.moderation.domain.Moderation.*;
import com.meridian.content.application.ContentAccess;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ModerationUseCasesTest {
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 @Test void unprivilegedQueueNeverLoadsReports(){
  var store=mock(ModerationStore.class);var service=new ModerationService(store,mock(ContentAccess.class),transactions);
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.queue("member",null)).reason());verify(store,never()).queue(any());
 }
 @Test void authorOrReporterCannotReviewEvenWhenOperator(){
  var store=mock(ModerationStore.class);var service=new ModerationService(store,mock(ContentAccess.class),transactions);var id=UUID.randomUUID();
  for(String actor:List.of("author","reporter")){
   when(store.operator(actor,true)).thenReturn(true);when(store.lockReport(id)).thenReturn(Optional.of(new ReviewTarget(UUID.randomUUID(),"author","reporter","OPEN",1,1)));
   assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.decide(actor,id,Decision.REMOVED,"reason",1,1)).reason());
  }
  verify(store,never()).decide(any(),any());verify(store,never()).removePost(any());verify(store,never()).audit(any(),anyString(),any(),anyString(),anyLong());
 }
 @Test void replayRequiresMatchingAuditAndDoesNotRepeatEffects(){
  var store=mock(ModerationStore.class);var service=new ModerationService(store,mock(ContentAccess.class),transactions);var id=UUID.randomUUID();
  when(store.operator("operator",true)).thenReturn(true);when(store.lockReport(id)).thenReturn(Optional.of(new ReviewTarget(UUID.randomUUID(),"author","reporter","REMOVED",2,4)));
  when(store.auditMatches(id,"operator",Decision.REMOVED,"reason",4)).thenReturn(true);
  service.decide("operator",id,Decision.REMOVED," reason ",1,4);
  assertThrows(UseCaseFailure.class,()->service.decide("operator",id,Decision.REMOVED,"different",1,4));
  verify(store,never()).decide(any(),any());verify(store,never()).removePost(any());verify(store,never()).audit(any(),anyString(),any(),anyString(),anyLong());
 }
 @Test void dailyLimitRejectsNewReportsButExactRetryIsAllowed(){
  var store=mock(ModerationStore.class);var access=mock(ContentAccess.class);var service=new ModerationService(store,access,transactions);var id=UUID.randomUUID();var post=UUID.randomUUID();
  when(access.lockVisible("reporter",post)).thenReturn(true);when(store.lockProfile("reporter")).thenReturn(true);when(store.dailyCount("reporter")).thenReturn(20);
  assertEquals(UseCaseFailure.Reason.RATE_LIMITED,assertThrows(UseCaseFailure.class,()->service.submit("reporter",id,post,Category.SPAM,"details")).reason());verify(store,never()).insert(any(),anyString(),any(),any(),anyString());
  var report=new Report(id,post,"SPAM","OPEN",1,"now");when(store.matching(id,"reporter",post,Category.SPAM,"details")).thenReturn(Optional.of(report));
  assertEquals(report,service.submit("reporter",id,post,Category.SPAM,"details"));
 }
}
