package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.exports.application.*;
import com.meridian.exports.domain.ExportWork;
import com.meridian.shared.application.Transactions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ExportGenerationUseCasesTest {
 @Test void retryPolicyStopsAfterThirdAttemptOrOversizedSnapshot(){
  var store=mock(ExportQueueStore.class);var tx=mock(Transactions.class);var queue=new ExportQueueService(store,tx);var id=UUID.randomUUID();var first=new ExportWork(id,"member",1);var third=new ExportWork(id,"member",3);
  queue.fail(first,false);verify(store).fail(first,"QUEUED",null);queue.fail(first,true);verify(store).fail(first,"FAILED","TOO_LARGE");queue.fail(third,false);verify(store).fail(third,"FAILED","GENERATION_FAILED");
 }
 @Test void staleCompletionDoesNotCountAsSuccess(){
  var queue=mock(ExportQueueUseCases.class);var snapshots=mock(ExportSnapshot.class);var metrics=mock(ExportMetrics.class);var work=new ExportWork(UUID.randomUUID(),"member",2);when(queue.claim()).thenReturn(work);when(snapshots.generate("member")).thenReturn(new byte[]{1});
  new ExportGenerationService(queue,snapshots,metrics).generateNext();verify(metrics,never()).completed();verify(metrics,never()).failed();
 }
 @Test void snapshotFailureClassifiesOversizeAndToleratesUnavailableRetryStore(){
  var queue=mock(ExportQueueUseCases.class);var snapshots=mock(ExportSnapshot.class);var metrics=mock(ExportMetrics.class);var work=new ExportWork(UUID.randomUUID(),"member",1);when(queue.claim()).thenReturn(work);when(snapshots.generate("member")).thenThrow(new ExportSnapshot.TooLarge());doThrow(new IllegalStateException("Unavailable")).when(queue).fail(work,true);
  assertDoesNotThrow(()->new ExportGenerationService(queue,snapshots,metrics).generateNext());verify(metrics).failed();verify(queue).fail(work,true);verify(queue,never()).complete(any(),any());
 }
 @Test void claimAndLeaseExecuteTogetherAfterExpiryRecovery(){
  var store=mock(ExportQueueStore.class);var work=new ExportWork(UUID.randomUUID(),"member",1);when(store.lockNext()).thenReturn(work);boolean[] active={false};
  Transactions tx=new Transactions(){public <T>T write(Supplier<T> operation){active[0]=true;try{return operation.get();}finally{active[0]=false;}}public <T>T snapshot(Supplier<T> operation){return operation.get();}};
  doAnswer(call->{assertTrue(active[0]);return null;}).when(store).lease(work);assertEquals(work,new ExportQueueService(store,tx).claim());var order=inOrder(store);order.verify(store).expire();order.verify(store).failExhausted();order.verify(store).lockNext();order.verify(store).lease(work);
 }
}
