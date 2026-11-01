package com.meridian;
import com.meridian.identity.application.*;
import com.meridian.identity.domain.SessionPolicy;
import com.meridian.identity.domain.Sessions.*;
import com.meridian.shared.application.Transactions;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class IdentityUseCasesTest {
 private final Instant now=Instant.parse("2026-11-01T12:00:00Z");
 private final SessionPolicy policy=new SessionPolicy(Duration.ofHours(12),Clock.fixed(now,ZoneOffset.UTC));
 private final Transactions transactions=new Transactions(){
  public <T>T write(Supplier<T> work){return work.get();}
  public <T>T snapshot(Supplier<T> work){return work.get();}
 };
 @Test void exactTimeBoundariesRemainInclusiveForExpiryAndFreshness(){
  assertTrue(policy.expired(now.minus(Duration.ofHours(12)).toEpochMilli()));assertFalse(policy.expired(now.minus(Duration.ofHours(12)).toEpochMilli()+1));
  assertTrue(policy.freshLogout(now.minusSeconds(300)));assertTrue(policy.freshLogout(now.plusSeconds(60)));assertFalse(policy.freshLogout(now.minusSeconds(301)));assertFalse(policy.freshLogout(now.plusSeconds(61)));assertFalse(policy.freshLogout(null));
  assertTrue(policy.recentlyAuthenticated(now.minusSeconds(300)));assertTrue(policy.recentlyAuthenticated(now.plusSeconds(30)));assertFalse(policy.recentlyAuthenticated(now.plusSeconds(31)));assertFalse(policy.recentlyAuthenticated(null));
 }
 @Test void redirectPolicyOnlyAcceptsTypedProfileIdsAndPrivacyTakesPrecedence(){
  var id=UUID.randomUUID();assertEquals("/members/"+id,SessionPolicy.returnPath(false,id));
  assertEquals("",SessionPolicy.returnPath(false,"https://untrusted.example"));assertEquals("",SessionPolicy.returnPath(false,id.toString()));assertEquals("/settings#privacy",SessionPolicy.returnPath(true,id));
 }
 @Test void providerMismatchCannotWriteBindingsOrRevocationEvents(){
  var store=mock(ProviderSessionStore.class);var service=new ProviderSessionService(store,transactions,policy);
  var claims=new Claims("issuer",List.of("client"),"subject","sid",now,"event");
  assertThrows(IllegalArgumentException.class,()->service.bind("session",claims,new Provider("other","client")));
  assertThrows(IllegalArgumentException.class,()->service.revoke(claims,new Provider("issuer","other")));
  verifyNoInteractions(store);
 }
 @Test void providerReplayDoesNotRepeatDeletionOrCountUpdate(){
  var store=mock(ProviderSessionStore.class);var service=new ProviderSessionService(store,transactions,policy);var claims=new Claims("issuer",List.of("client"),"subject","sid",now,"event");var provider=new Provider("issuer","client");
  when(store.recordLogout(claims,provider)).thenReturn(true,false);when(store.deleteBoundSessions(claims,provider)).thenReturn(2);
  service.revoke(claims,provider);service.revoke(claims,provider);
  verify(store,times(1)).deleteBoundSessions(claims,provider);verify(store,times(1)).recordCount(claims,provider,2);
 }
 @Test void revokedProviderSessionIsDeniedWithoutTrustingSessionPersistence(){
  var store=mock(SessionStore.class);var providers=mock(ProviderSessionUseCases.class);var service=new SessionService(store,providers,transactions,policy);
  when(providers.revoked("session")).thenReturn(true);assertFalse(service.active("session","subject"));verifyNoInteractions(store);
 }
 @Test void sessionListCapsExpiryAndDoesNotReturnCookieIdentifiers(){
  var store=mock(SessionStore.class);var service=new SessionService(store,mock(ProviderSessionUseCases.class),transactions,policy);var id=UUID.randomUUID();long created=now.minusSeconds(60).toEpochMilli();
  when(store.list(eq("subject"),eq("cookie-id"),anyLong(),anyLong())).thenReturn(List.of(new SessionRow(id,created,now.toEpochMilli(),now.plus(Duration.ofDays(2)).toEpochMilli(),true)));
  var result=service.list("subject","cookie-id").getFirst();assertEquals(id,result.id());assertEquals(Instant.ofEpochMilli(policy.deadline(created)),result.expiresAt());assertTrue(result.current());
 }
 @Test void foreignSessionRevocationIsIdempotentAndDoesNotEmitAudit(){
  var store=mock(SessionStore.class);var service=new SessionService(store,mock(ProviderSessionUseCases.class),transactions,policy);var id=UUID.randomUUID();
  service.revoke("owner",id);verify(store).revoke("owner",id);verify(store,never()).audit(anyString(),any(),anyInt());
  when(store.revokeAll("owner")).thenReturn(3);service.revokeAll("owner");verify(store).audit("owner",Revocation.REVOKE_ALL,3);
 }
}
