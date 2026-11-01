package com.meridian;
import com.meridian.identity.adapter.ProviderSessions;
import com.meridian.identity.adapter.SessionLifetimeFilter;
import com.meridian.identity.domain.SessionPolicy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import static org.junit.jupiter.api.Assertions.*;

class SessionLifetimeTest {
 @Test void pollingCannotKeepAnOldAuthenticatedSessionAlive()throws Exception{
  var lifetime=new SessionPolicy(Duration.ofHours(12),java.time.Clock.systemUTC());
  var session=new MockHttpSession(){@Override public long getCreationTime(){return System.currentTimeMillis()-Duration.ofHours(13).toMillis();}};
  var request=new MockHttpServletRequest();request.setSession(session);var response=new MockHttpServletResponse();
  var token=OidcIdToken.withTokenValue("test").claim(IdTokenClaimNames.SUB,"member").build();
  var principal=new DefaultOidcUser(java.util.List.of(),token);
  SecurityContextHolder.getContext().setAuthentication(new OAuth2AuthenticationToken(principal,java.util.List.of(),"keycloak"));
  try{
   new SessionLifetimeFilter(lifetime,org.mockito.Mockito.mock(ProviderSessions.class)).doFilter(request,response,(q,s)->fail("Expired request must not reach the API"));
   assertEquals(401,response.getStatus());assertTrue(session.isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());
  }finally{SecurityContextHolder.clearContext();}
 }
 @Test void policyIsBoundedAndRecentSessionsRemainValid(){
  var policy=new SessionPolicy(Duration.ofHours(12),java.time.Clock.systemUTC());assertFalse(policy.expired(System.currentTimeMillis()));
  assertTrue(policy.expired(System.currentTimeMillis()-Duration.ofDays(1).toMillis()));
  assertThrows(IllegalArgumentException.class,()->new SessionPolicy(Duration.ZERO,java.time.Clock.systemUTC()));
  assertThrows(IllegalArgumentException.class,()->new SessionPolicy(Duration.ofDays(8),java.time.Clock.systemUTC()));
 }
}
