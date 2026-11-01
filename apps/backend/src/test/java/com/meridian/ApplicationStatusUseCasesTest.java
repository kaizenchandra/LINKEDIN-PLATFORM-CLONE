package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.applications.application.*;
import com.meridian.applications.domain.ApplicationStatus;
import com.meridian.applications.domain.Applications.State;
import com.meridian.shared.application.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ApplicationStatusUseCasesTest {
 private final ApplicationStatusStore store=mock(ApplicationStatusStore.class);
 private final ApplicationSignals signals=mock(ApplicationSignals.class);
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final ApplicationStatusService service=new ApplicationStatusService(store,signals,transactions);
 private final UUID id=UUID.randomUUID(),company=UUID.randomUUID();
 private void accessible(String subject){when(store.organization(id,subject)).thenReturn(Optional.of(company));when(store.lockOrganization(company)).thenReturn(true);when(store.lockStatus(id,subject)).thenReturn(Optional.of(new ApplicationStatus("applicant",State.SUBMITTED,1)));}
 @Test void permissionIsRecheckedAfterOrganizationLock(){
  accessible("recruiter");when(store.lockStatus(id,"recruiter")).thenReturn(Optional.empty());
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.change("recruiter",id,State.REVIEWING,1)).reason());verify(store,never()).update(any(),any());verifyNoInteractions(signals);
 }
 @Test void wrongActorAndStaleVersionsCannotWrite(){
  accessible("applicant");accessible("recruiter");
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.change("applicant",id,State.ACCEPTED,1)).reason());
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.change("recruiter",id,State.WITHDRAWN,1)).reason());
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.change("recruiter",id,State.REVIEWING,2)).reason());verify(store,never()).update(any(),any());verifyNoInteractions(signals);
 }
 @Test void auditOutboxFailurePreventsSignalAndReceipt(){
  accessible("recruiter");doThrow(new IllegalStateException("outbox unavailable")).when(store).auditAndOutbox(id,2,"recruiter",State.SUBMITTED,State.REVIEWING);
  assertThrows(IllegalStateException.class,()->service.change("recruiter",id,State.REVIEWING,1));verifyNoInteractions(signals);verify(store,never()).receipt(any(),anyString());
 }
 @Test void transitionMatrixPreservesTerminalStatesAndSelfReviewRule(){
  for(State from:State.values())for(State to:State.values()){
   var status=new ApplicationStatus("applicant",from,1);
   boolean applicant=to==State.WITHDRAWN&&(from==State.SUBMITTED||from==State.REVIEWING);
   boolean recruiter=from==State.SUBMITTED&&(to==State.REVIEWING||to==State.REJECTED)||from==State.REVIEWING&&(to==State.ACCEPTED||to==State.REJECTED);
   assertEquals(applicant,status.actorAllowed("applicant",to)&&status.canTransition("applicant",to));
   assertEquals(recruiter,status.actorAllowed("recruiter",to)&&status.canTransition("recruiter",to));
  }
 }
}
