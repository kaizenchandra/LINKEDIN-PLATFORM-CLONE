package com.meridian;
import java.util.*;
import com.meridian.messaging.application.*;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.shared.application.UseCaseFailure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MessagingReadUseCasesTest {
 private final MessagingReadStore store=mock(MessagingReadStore.class);
 private final MessagingReadService service=new MessagingReadService(store);
 @Test void invalidCursorAndSearchCannotReachPersistence(){
  assertThrows(UseCaseFailure.class,()->service.list("a",0L,View.INBOX,""));assertThrows(UseCaseFailure.class,()->service.history("a",UUID.randomUUID(),-1L));assertThrows(UseCaseFailure.class,()->service.list("a",null,View.INBOX,"x".repeat(121)));verifyNoInteractions(store);
 }
 @Test void unavailableConversationCannotLoadPrivateMessages(){
  var id=UUID.randomUUID();when(store.access(id,"outsider")).thenThrow(new UseCaseFailure(UseCaseFailure.Reason.NOT_FOUND,"Conversation or connection unavailable"));
  assertThrows(UseCaseFailure.class,()->service.history("outsider",id,null));verify(store,never()).messages(any(),anyString(),any(),any());
 }
 @Test void historyPreservesSampledWatermarkAndReversesOnlyVisiblePage(){
  var id=UUID.randomUUID();var preference=new Preference(false,false,0L);var access=new Access("a","b",100,10,70,preference,preference);var person=new Person(UUID.randomUUID(),"Member");var rows=new ArrayList<Message>();
  for(long seq=100;seq>=50;seq--)rows.add(new Message(UUID.randomUUID(),seq,"Text",true,seq<=70,"2026-01-01T00:00:00Z"));
  when(store.access(id,"a")).thenReturn(access);when(store.messages(id,"a",null,access)).thenReturn(rows);when(store.person("b")).thenReturn(person);when(store.connected("a","b")).thenReturn(true);
  var result=service.history("a",id,null);assertEquals(50,result.items().size());assertEquals(51,result.items().getFirst().sequence());assertEquals(100,result.items().getLast().sequence());assertEquals("51",result.nextCursor());assertEquals(100,result.latestSequence());assertEquals(70,result.recipientReadSequence());assertTrue(result.canSend());
 }
 @Test void conversationCursorUsesLastVisibleSequence(){
  var rows=new ArrayList<ConversationRow>();for(long seq=21;seq>=1;seq--)rows.add(new ConversationRow(new Conversation(UUID.randomUUID(),UUID.randomUUID(),"Member",0,true,new Preference(false,false,0L)),seq));
  when(store.list("a",null,View.STARRED,"Member")).thenReturn(rows);var result=service.list("a",null,View.STARRED," Member ");assertEquals(20,result.items().size());assertEquals("2",result.nextCursor());
 }
}
