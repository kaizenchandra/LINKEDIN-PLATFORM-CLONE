package com.meridian;
import com.meridian.content.application.*;
import com.meridian.content.domain.Content.*;
import com.meridian.shared.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ContentUseCasesTest {
 private final Transactions transactions=new Transactions(){
  public <T>T write(Supplier<T> work){return work.get();}
  public <T>T snapshot(Supplier<T> work){return work.get();}
 };
 @Test void hiddenOrUnavailablePostRejectsReactionBeforeAnyWrite(){
  var store=mock(PostStore.class);var access=mock(ContentAccess.class);var service=new PostService(store,access,transactions);var id=UUID.randomUUID();
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.react("member",id,true)).reason());verifyNoInteractions(store);
 }
 @Test void invalidTextAndCursorNeverReachPersistence(){
  var store=mock(PostStore.class);var service=new PostService(store,mock(ContentAccess.class),transactions);
  assertThrows(UseCaseFailure.class,()->service.save("member",UUID.randomUUID(),"  ",Audience.MEMBERS,0));
  assertThrows(UseCaseFailure.class,()->service.feed("member",0L,FeedScope.RECENT));verifyNoInteractions(store);
 }
 @Test void privateDraftCapacityStopsCreationButOwnedRetryStillSucceeds(){
  var store=mock(DraftStore.class);var service=new DraftService(store,mock(PostUseCases.class),transactions);var id=UUID.randomUUID();
  when(store.lockProfile("owner")).thenReturn(true);when(store.count("owner")).thenReturn(100);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.save("owner",id,"text",Audience.MEMBERS,0)).reason());
  verify(store,never()).insert(anyString(),any(),anyString(),any());
  var draft=new Draft(id,"text",Audience.MEMBERS,1,false,"2026-11-01T00:00:00Z");when(store.exists(id)).thenReturn(true);when(store.owned("owner",id,true)).thenReturn(Optional.of(draft));
  assertEquals(draft,service.save("owner",id,"text",Audience.MEMBERS,0));
 }
 @Test void publishRetryReadsCurrentAuthorizedPostAndNeverInsertsTwice(){
  var store=mock(DraftStore.class);var posts=mock(PostUseCases.class);var service=new DraftService(store,posts,transactions);var id=UUID.randomUUID();
  when(store.owned("owner",id,true)).thenReturn(Optional.of(new Draft(id,"text",Audience.MEMBERS,2,true,"now")));
  service.publish("owner",id,1);verify(posts).get("owner",id);verify(store,never()).createPost(anyString(),any());verify(store,never()).markPublished(any());
  assertThrows(UseCaseFailure.class,()->service.publish("owner",id,2));
 }
 @Test void blankDraftCannotPublish(){
  var store=mock(DraftStore.class);var posts=mock(PostUseCases.class);var service=new DraftService(store,posts,transactions);var id=UUID.randomUUID();
  when(store.owned("owner",id,true)).thenReturn(Optional.of(new Draft(id,"  ",Audience.MEMBERS,1,false,"now")));
  assertThrows(UseCaseFailure.class,()->service.publish("owner",id,1));verify(store,never()).createPost(anyString(),any());verifyNoInteractions(posts);
 }
 @Test void commentParentMustBeVisibleBeforeInsertion(){
  var store=mock(CommentStore.class);var access=mock(ContentAccess.class);var service=new CommentService(store,access,transactions);var post=UUID.randomUUID();var parent=UUID.randomUUID();
  when(access.lockVisible("member",post)).thenReturn(true);
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.create("member",post,UUID.randomUUID(),"reply",parent)).reason());
  verify(store,never()).insert(anyString(),any(),any(),anyString(),any());
 }
}
