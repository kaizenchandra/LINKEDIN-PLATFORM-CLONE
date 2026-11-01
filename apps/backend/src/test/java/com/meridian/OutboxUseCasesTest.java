package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.events.application.*;
import com.meridian.events.domain.OutboxEvent;
import com.meridian.shared.application.Transactions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OutboxUseCasesTest {
 private final OutboxStore store=mock(OutboxStore.class);
 private final OutboxPublisher publisher=mock(OutboxPublisher.class);
 private final OutboxMetrics metrics=mock(OutboxMetrics.class);
 private boolean inTransaction;
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){inTransaction=true;try{return work.get();}finally{inTransaction=false;}}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final OutboxService service=new OutboxService(store,publisher,metrics,transactions);
 private final OutboxEvent first=new OutboxEvent(UUID.randomUUID(),UUID.randomUUID(),null,null),second=new OutboxEvent(UUID.randomUUID(),null,UUID.randomUUID(),1L);
 @Test void acknowledgementPrecedesPublishedMarkerWithinTransaction()throws Exception{
  when(store.lockReady()).thenReturn(List.of(first));doAnswer(call->{assertTrue(inTransaction);return null;}).when(publisher).publish(first);
  service.publishReady();var order=inOrder(store,publisher,metrics);order.verify(store).lockReady();order.verify(publisher).publish(first);order.verify(store).published(first.id());order.verify(metrics).published();assertFalse(inTransaction);
 }
 @Test void failedSendSchedulesRetryAndStopsBatch()throws Exception{
  when(store.lockReady()).thenReturn(List.of(first,second));doThrow(new java.io.IOException("Broker unavailable")).when(publisher).publish(first);
  service.publishReady();verify(store).retry(first.id());verify(metrics).retry();verify(store,never()).published(any());verify(publisher,never()).publish(second);
 }
 @Test void retryPersistenceFailureEscapesForTransactionRollback()throws Exception{
  when(store.lockReady()).thenReturn(List.of(first));doThrow(new java.io.IOException("Broker unavailable")).when(publisher).publish(first);doThrow(new IllegalStateException("Database unavailable")).when(store).retry(first.id());
  assertThrows(IllegalStateException.class,service::publishReady);verifyNoInteractions(metrics);
 }
}
