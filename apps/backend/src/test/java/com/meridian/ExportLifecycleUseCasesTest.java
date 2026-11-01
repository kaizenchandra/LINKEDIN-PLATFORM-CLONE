package com.meridian;
import java.util.*;
import java.time.*;
import java.util.function.Supplier;
import com.meridian.exports.application.*;
import com.meridian.exports.domain.ExportRecord;
import com.meridian.shared.application.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ExportLifecycleUseCasesTest {
 private final ExportLifecycleStore store=mock(ExportLifecycleStore.class);
 private final Instant now=Instant.parse("2026-01-01T00:00:00Z");
 private final Transactions tx=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final ExportLifecycleService service=new ExportLifecycleService(store,tx,Clock.fixed(now,ZoneOffset.UTC));
 private final UUID id=UUID.randomUUID();
 @Test void replayReturnsOwnedExportBeforePendingOrThrottleChecks(){
  var item=new ExportRecord(id,"QUEUED",now,null,null);when(store.lockProfile("member")).thenReturn(true);when(store.owned("member",id)).thenReturn(Optional.of(item));assertEquals(item,service.request("member",id).export());verify(store,never()).pending(anyString());verify(store,never()).recent(anyString());verify(store,never()).audit(any(),anyString());
 }
 @Test void throttleDoesNotCreateOrAudit(){when(store.lockProfile("member")).thenReturn(true);when(store.recent("member")).thenReturn(true);assertTrue(service.request("member",id).throttled());verify(store,never()).insert(anyString(),any());verify(store,never()).audit(any(),anyString());}
 @Test void expiryRaceCannotReturnPayloadOrAudit(){
  when(store.owned("member",id)).thenReturn(Optional.of(new ExportRecord(id,"READY",now,now.plusSeconds(1),null)));assertEquals(UseCaseFailure.Reason.GONE,assertThrows(UseCaseFailure.class,()->service.download("member",id)).reason());verify(store,never()).audit(any(),anyString());
  when(store.owned("member",id)).thenReturn(Optional.of(new ExportRecord(id,"READY",now,now.minusSeconds(1),null)));clearInvocations(store);assertThrows(UseCaseFailure.class,()->service.download("member",id));verify(store,never()).lockPayload(anyString(),any());
 }
 @Test void unavailableCancellationAndRepeatedCancellationDoNotAudit(){assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.cancel("other",id)).reason());when(store.owned("member",id)).thenReturn(Optional.of(new ExportRecord(id,"EXPIRED",now,now,null)));service.cancel("member",id);verify(store,never()).audit(any(),anyString());}
}
