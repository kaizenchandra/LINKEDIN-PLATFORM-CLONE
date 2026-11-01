package com.meridian;
import com.meridian.notifications.application.*;
import com.meridian.notifications.domain.Notifications.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationUseCasesTest {
 private final Transactions transactions=new Transactions(){public <T>T write(Supplier<T> work){return work.get();}public <T>T snapshot(Supplier<T> work){return work.get();}};
 @Test void invalidOrOverflowingReadBoundaryCannotReachPersistence(){
  var store=mock(NotificationStore.class);var signals=mock(NotificationSignals.class);var service=new NotificationService(store,signals,transactions);
  for(String boundary:new String[]{"0","-1","01","9223372036854775808","1.5"," 1",""})assertEquals(UseCaseFailure.Reason.INVALID,assertThrows(UseCaseFailure.class,()->service.readAll("owner",boundary)).reason());
  verifyNoInteractions(store,signals);
  service.readAll("owner",Long.toString(Long.MAX_VALUE));verify(store).readAll("owner",Long.MAX_VALUE);verify(signals).changed("owner");
 }
 @Test void unavailableNotificationNeverEmitsOwnerSignal(){
  var store=mock(NotificationStore.class);var signals=mock(NotificationSignals.class);var service=new NotificationService(store,signals,transactions);var id=UUID.randomUUID();
  for(var change:Change.values())assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.change("owner",id,change)).reason());
  verifyNoInteractions(signals);
 }
 @Test void preferencesDefaultWithoutWritingAndStaleMutationDoesNotSignal(){
  var store=mock(NotificationStore.class);var signals=mock(NotificationSignals.class);var service=new NotificationService(store,signals,transactions);
  assertEquals(new Preference(true,true,0),service.preferences("owner"));
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.preferences("owner",false,null,7)).reason());verifyNoInteractions(signals);
 }
 @Test void categoryPageKeepsGlobalUnreadCountAndBoundary(){
  var store=mock(NotificationStore.class);var service=new NotificationService(store,mock(NotificationSignals.class),transactions);
  var rows=new ArrayList<Row>();for(int i=0;i<21;i++)rows.add(new Row(new Notification(UUID.randomUUID(),"Actor","now",false,"CONNECTION_ACCEPTED",null,null,null,null,null),100-i));
  when(store.list("owner",null,Category.NETWORK)).thenReturn(rows);when(store.unreadCount("owner")).thenReturn(47L);when(store.readThrough("owner")).thenReturn(190L);
  var page=service.list("owner",null,Category.NETWORK);assertEquals(20,page.items().size());assertEquals("81",page.nextCursor());assertEquals(47,page.unreadCount());assertEquals("190",page.readThrough());
 }
}
