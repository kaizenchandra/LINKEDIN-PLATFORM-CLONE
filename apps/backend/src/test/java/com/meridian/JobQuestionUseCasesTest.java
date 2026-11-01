package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.jobs.application.*;
import com.meridian.jobs.domain.JobQuestions;
import com.meridian.jobs.domain.JobQuestions.*;
import com.meridian.shared.application.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class JobQuestionUseCasesTest {
 private final JobQuestionStore store=mock(JobQuestionStore.class);
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final JobQuestionService service=new JobQuestionService(store,transactions);
 private final UUID job=UUID.randomUUID(),company=UUID.randomUUID();
 private void authorized(){when(store.organization(job)).thenReturn(Optional.of(company));when(store.lockOrganization(company)).thenReturn(true);when(store.canRecruit(company,"owner")).thenReturn(true);}
 @Test void unavailableJobAndUnauthorizedRecruiterCannotWrite(){
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.get(job)).reason());
  when(store.organization(job)).thenReturn(Optional.of(company));when(store.lockOrganization(company)).thenReturn(true);
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.save("other",job,new Form(0,List.of()))).reason());verify(store,never()).lockJob(any());verify(store,never()).save(any(),any(),any());
 }
 @Test void staleVersionCannotReplaceQuestions(){
  authorized();when(store.load(job)).thenReturn(new Form(2,List.of()));
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.save("owner",job,new Form(1,List.of()))).reason());verify(store,never()).save(any(),any(),any());
 }
 @Test void normalizesOnlyAfterLocksAndReturnsPersistedRevision(){
  authorized();var id=UUID.randomUUID();var input=new Question(id,"  Why this role?  ",true);var normalized=new Question(id,"Why this role?",true);var current=new Form(0,List.of());var saved=new Form(1,List.of(normalized));when(store.load(job)).thenReturn(current,saved);
  assertEquals(saved,service.save("owner",job,new Form(0,List.of(input))));
  var order=inOrder(store);order.verify(store).organization(job);order.verify(store).lockOrganization(company);order.verify(store).canRecruit(company,"owner");order.verify(store).lockJob(job);order.verify(store).load(job);order.verify(store).save(job,current,List.of(normalized));order.verify(store).organization(job);order.verify(store).load(job);
 }
 @Test void rejectsDuplicatesControlsAndOversizedForms(){
  var q=new Question(UUID.randomUUID(),"Question",false);
  assertThrows(IllegalArgumentException.class,()->JobQuestions.normalize(List.of(q,q)));
  assertThrows(IllegalArgumentException.class,()->JobQuestions.normalize(List.of(new Question(q.id(),"Bad\u0000text",true))));
  assertThrows(IllegalArgumentException.class,()->JobQuestions.normalize(Collections.nCopies(6,q)));
  assertThrows(IllegalArgumentException.class,()->JobQuestions.normalize(List.of(new Question(q.id(),"x".repeat(301),true))));
  assertEquals(List.of(),JobQuestions.normalize(List.of()));
 }
}
