package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.notifications.application.*;
import com.meridian.notifications.domain.NotificationSource;
import com.meridian.shared.application.Transactions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationProjectionUseCasesTest {
 private final NotificationProjectionStore store=mock(NotificationProjectionStore.class);
 private final NotificationSignals signals=mock(NotificationSignals.class);
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final NotificationProjectionService service=new NotificationProjectionService(store,signals,transactions);
 private final UUID event=UUID.randomUUID(),application=UUID.randomUUID();
 @Test void unmatchedApplicationEventRemainsRetryable(){
  service.applicationChanged(event,application,1);verify(store,never()).claim(any());verifyNoInteractions(signals);
 }
 @Test void optedOutApplicationIsConsumedWithoutNotification(){
  when(store.source(event,application,1)).thenReturn(Optional.of(new NotificationSource("applicant","owner","actor","SUBMITTED")));when(store.claim(event)).thenReturn(true);
  service.applicationChanged(event,application,1);verify(store).jobsEnabled("owner");verify(store,never()).insertApplication(any(),any(),anyLong(),anyString(),anyString());verifyNoInteractions(signals);
 }
 @Test void duplicateConnectionDoesNotProjectOrSignal(){
  service.connectionAccepted(event,application);verify(store,never()).insertConnection(any(),any());verifyNoInteractions(signals);
 }
 @Test void statusSelectsAuthoritativeAudienceAndSignalsOnlyInsertedRows(){
  for(String status:List.of("SUBMITTED","WITHDRAWN"))assertEquals("owner",new NotificationSource("applicant","owner","actor",status).recipient());
  when(store.source(event,application,2)).thenReturn(Optional.of(new NotificationSource("applicant","owner","actor","REVIEWING")));when(store.claim(event)).thenReturn(true);when(store.jobsEnabled("applicant")).thenReturn(true);
  service.applicationChanged(event,application,2);verifyNoInteractions(signals);
  when(store.insertApplication(event,application,2,"applicant","actor")).thenReturn(true);service.applicationChanged(event,application,2);verify(signals).changed("applicant");
 }
}
