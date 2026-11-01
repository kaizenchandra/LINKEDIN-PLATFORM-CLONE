package com.meridian;
import com.meridian.jobs.application.*;
import com.meridian.jobs.domain.Jobs.*;
import com.meridian.jobs.domain.Workplace;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class JobUseCasesTest {
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 Write input(UUID company,Status status,long version){return new Write(company,"Engineer","City","Role",Workplace.REMOTE,status,version);}
 @Test void deniedRecruiterCannotInsertOrEdit(){
  var store=mock(JobStore.class);var service=new JobService(store,transactions);var company=UUID.randomUUID();when(store.lockOrganization(company)).thenReturn(true);
  assertEquals(UseCaseFailure.Reason.FORBIDDEN,assertThrows(UseCaseFailure.class,()->service.publish("member",UUID.randomUUID(),input(company,Status.OPEN,0))).reason());verify(store,never()).insert(any(),any());verify(store,never()).update(any(),any());
 }
 @Test void newJobMustBeOpenAndCompanyCannotChangeDuringEdit(){
  var store=mock(JobStore.class);var service=new JobService(store,transactions);var company=UUID.randomUUID();var id=UUID.randomUUID();when(store.lockOrganization(company)).thenReturn(true);when(store.recruitingAllowed("member",company)).thenReturn(true);
  assertEquals(UseCaseFailure.Reason.INVALID,assertThrows(UseCaseFailure.class,()->service.publish("member",id,input(company,Status.CLOSED,0))).reason());
  when(store.lockJobOrganization(id)).thenReturn(Optional.of(UUID.randomUUID()));
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.publish("member",id,input(company,Status.OPEN,1))).reason());verify(store,never()).update(any(),any());
 }
 @Test void creationReplayDoesNotEditAndRemovingMarkersNeedsNoExistingJob(){
  var store=mock(JobStore.class);var service=new JobService(store,transactions);var company=UUID.randomUUID();var id=UUID.randomUUID();when(store.lockOrganization(company)).thenReturn(true);when(store.recruitingAllowed("member",company)).thenReturn(true);when(store.lockJobOrganization(id)).thenReturn(Optional.of(company));
  var job=new Job(id,company,"Company","Engineer","City","Role",Workplace.REMOTE,Status.OPEN,1,true,false,true);when(store.get("member",id)).thenReturn(Optional.of(job));
  assertEquals(job,service.publish("member",id,input(company,Status.OPEN,0)));verify(store,never()).update(any(),any());
  clearInvocations(store);service.marker("member",id,Marker.SAVED,false);verify(store).marker("member",id,Marker.SAVED,false);verify(store,never()).get(anyString(),any());verify(store,never()).initialized(anyString());
 }
}
