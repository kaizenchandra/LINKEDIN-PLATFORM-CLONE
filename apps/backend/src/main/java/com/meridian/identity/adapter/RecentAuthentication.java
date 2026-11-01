package com.meridian.identity.adapter;
import com.meridian.identity.domain.SessionPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public final class RecentAuthentication {
 private final SessionPolicy policy;
 public RecentAuthentication(SessionPolicy policy){this.policy=policy;}
 public void require(OidcUser user){
  if(!policy.recentlyAuthenticated(user.getIdToken().getAuthenticatedAt()))throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,"Verify your identity again before continuing");
 }
}
