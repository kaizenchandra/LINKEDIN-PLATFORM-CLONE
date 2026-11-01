package com.meridian;
import java.util.*;
import com.meridian.events.application.*;
import com.meridian.events.domain.EventDelivery;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class EventConsumptionUseCasesTest {
 private final EventProcessor processor=mock(EventProcessor.class);
 private final DeliveryOffsets offsets=mock(DeliveryOffsets.class);
 private final EventConsumptionService service=new EventConsumptionService(processor);
 private EventDelivery delivery(int partition,long offset){return new EventDelivery("topic",partition,offset,"key","payload");}
 @Test void commitFollowsSuccessfulProcessing()throws Exception{
  var record=delivery(0,7);service.consume(List.of(record),offsets);var order=inOrder(processor,offsets);order.verify(processor).process(record);order.verify(offsets).commit(record.source(),8);verify(offsets,never()).seek(any(),anyLong());
 }
 @Test void failedProcessingRewindsEveryUncommittedPartitionWithoutReplayingCommittedRows()throws Exception{
  var a=delivery(0,7);var b=delivery(0,8);var c=delivery(1,20);var d=delivery(1,21);doThrow(new IllegalStateException("projection unavailable")).when(processor).process(b);
  assertThrows(IllegalStateException.class,()->service.consume(List.of(a,b,c,d),offsets));verify(offsets).commit(a.source(),8);verify(offsets).seek(a.source(),8);verify(offsets).seek(c.source(),20);verify(processor,never()).process(c);verify(offsets,never()).commit(b.source(),9);
 }
 @Test void failedCommitReplaysAlreadyProcessedEvent()throws Exception{
  var record=delivery(0,7);doThrow(new IllegalStateException("commit unavailable")).when(offsets).commit(record.source(),8);
  assertThrows(IllegalStateException.class,()->service.consume(List.of(record),offsets));verify(processor).process(record);verify(offsets).seek(record.source(),7);
 }
 @Test void failedQuarantineCannotAdvanceOffset()throws Exception{
  var record=delivery(0,7);doThrow(new java.io.IOException("DLQ unavailable")).when(processor).process(record);
  assertThrows(java.io.IOException.class,()->service.consume(List.of(record),offsets));verify(offsets,never()).commit(any(),anyLong());verify(offsets).seek(record.source(),7);
 }
}
