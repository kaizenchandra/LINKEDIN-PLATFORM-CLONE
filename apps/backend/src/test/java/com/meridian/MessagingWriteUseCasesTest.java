package com.meridian;
import java.util.*;
import java.util.function.Supplier;
import com.meridian.messaging.application.*;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.messaging.domain.PreviousMessage;
import com.meridian.shared.application.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MessagingWriteUseCasesTest {
 private final MessagingWriteStore store=mock(MessagingWriteStore.class);
 private final MessagingSignals signals=mock(MessagingSignals.class);
 private final Transactions tx=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 private final MessagingWriteService service=new MessagingWriteService(store,signals,tx);
 private final UUID id=UUID.randomUUID(),message=UUID.randomUUID();
 private void access(){var p=new Preference(false,false,0L);when(store.access(id,"a",true)).thenReturn(new Access("a","b",5,0,0,p,p));when(store.connected("a","b")).thenReturn(true);}
 @Test void identicalRetryDoesNotAdvanceOrSignal(){access();when(store.previous(message)).thenReturn(Optional.of(new PreviousMessage(id,"a","Text")));service.send("a",id,message," Text ");verify(store,never()).advance(any());verifyNoInteractions(signals);assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.send("a",id,message,"Changed")).reason());}
 @Test void idCollisionDoesNotAdvanceOrSignal(){access();assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.send("a",id,message,"Text")).reason());verify(store).insert(message,id,6,"a","Text");verify(store,never()).advance(any());verifyNoInteractions(signals);}
 @Test void readCannotExceedSampledSequenceAndStalePreferenceCannotSignal(){access();assertThrows(UseCaseFailure.class,()->service.read("a",id,6));verify(store,never()).read(any(),anyBoolean(),anyLong());assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.preferences("a",id,new Preference(true,true,0L))).reason());verifyNoInteractions(signals);}
 @Test void blockedPairCannotStartConversation(){var member=UUID.randomUUID();when(store.target(member,"a")).thenReturn(Optional.of("b"));when(store.connected("a","b")).thenReturn(true);when(store.blocked("a","b")).thenReturn(true);assertThrows(UseCaseFailure.class,()->service.start("a",member));verify(store).lockPair("a","b");verify(store,never()).start(anyString(),anyString());verifyNoInteractions(signals);}
}
