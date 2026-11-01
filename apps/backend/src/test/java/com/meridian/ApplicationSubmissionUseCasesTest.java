package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.applications.application.*;
import com.meridian.applications.domain.Submission;
import com.meridian.applications.domain.Submission.*;
import com.meridian.applications.domain.Applications.AnswerSnapshot;
import com.meridian.jobs.domain.JobQuestions.*;
import com.meridian.shared.application.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ApplicationSubmissionUseCasesTest {
 private final ApplicationSubmissionStore store=mock(ApplicationSubmissionStore.class);
 private final ApplicationSignals signals=mock(ApplicationSignals.class);
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final ApplicationSubmissionService service=new ApplicationSubmissionService(store,signals,transactions);
 private final UUID job=UUID.randomUUID(),company=UUID.randomUUID();
 private void available(){when(store.organization(job)).thenReturn(Optional.of(company));when(store.lockOrganization(company)).thenReturn(true);when(store.lockJob(job)).thenReturn(Optional.of("OPEN"));}
 @Test void retryUsesOriginalSnapshotEvenIfJobClosed(){
  available();when(store.lockJob(job)).thenReturn(Optional.of("CLOSED"));var id=UUID.randomUUID();var q=UUID.randomUUID();
  when(store.previous(job,"member")).thenReturn(Optional.of(new Previous(id,"Statement",1,List.of(new AnswerSnapshot(q,"Original",true,"Answer")),null)));
  service.submit("member",job,new Input(" Statement ",null,1L,List.of(new Answer(q," Answer "))));
  verify(store).receipt(id,"member");verify(store,never()).questions(any());verify(store,never()).auditAndOutbox(any(),anyString());verifyNoInteractions(signals);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.submit("member",job,new Input("Changed",null,1L,List.of()))).reason());
 }
 @Test void staleFormAndMissingRequiredAnswerRetainDifferentErrors(){
  available();when(store.questions(job)).thenReturn(new Form(1,List.of(new Question(UUID.randomUUID(),"Required",true))));
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.submit("member",job,new Input("Statement",null,0L,null))).reason());
  assertEquals(UseCaseFailure.Reason.UNPROCESSABLE,assertThrows(UseCaseFailure.class,()->service.submit("member",job,new Input("Statement",null,1L,null))).reason());verifyNoInteractions(signals);
 }
 @Test void answerValidationRejectsDuplicateUnknownAndControlCharacters(){
  var id=UUID.randomUUID();var form=new Form(1,List.of(new Question(id,"Question",false)));
  assertThrows(IllegalArgumentException.class,()->Submission.answer(form,List.of(new Answer(id,"One"),new Answer(id,"Two"))));
  assertThrows(IllegalArgumentException.class,()->Submission.answer(form,List.of(new Answer(UUID.randomUUID(),"Unknown"))));
  assertThrows(IllegalArgumentException.class,()->Submission.answer(form,List.of(new Answer(id,"Bad\u0000"))));
  assertEquals(List.of(new AnswerSnapshot(id,"Question",false,"")),Submission.answer(form,null));
 }
 @Test void resumeValidatesBytesAndFilenameWithoutChangingContent(){
  assertEquals(" Résumé\n",new Resume("resume.TXT"," Résumé\n").validated());
  assertThrows(IllegalArgumentException.class,()->new Resume("../resume.txt","Text").validated());
  assertThrows(IllegalArgumentException.class,()->new Resume("resume.txt","é".repeat(10241)).validated());
  assertThrows(IllegalArgumentException.class,()->new Resume("resume.txt","bad\u0000").validated());
 }
}
