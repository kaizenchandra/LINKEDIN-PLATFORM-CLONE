package com.meridian.identity.adapter;
import com.meridian.identity.domain.SessionPolicy;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.client.oidc.authentication.logout.OidcLogoutToken;
import org.springframework.stereotype.Component;
/** Servlet/OIDC boundary; session hints never become arbitrary redirect URLs. */
@Component
public final class BrowserAuthenticationHandlers {
 private final ProviderSessions sessions;
 public BrowserAuthenticationHandlers(ProviderSessions sessions){this.sessions=sessions;}
 public void failure(HttpServletRequest request,HttpServletResponse response,AuthenticationException error)throws IOException{
  org.slf4j.LoggerFactory.getLogger(BrowserAuthenticationHandlers.class).warn("OIDC login failed: {}",error instanceof OAuth2AuthenticationException oauth?oauth.getError().getErrorCode():"authentication_failed");
  response.sendRedirect("http://localhost:3100/?authentication=failed");
 }
 public void success(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException{
  var session=request.getSession(false);Object target=session==null?null:session.getAttribute("profileReturnId");boolean privacy=session!=null&&Boolean.TRUE.equals(session.getAttribute("privacyReturn"));
  if(session!=null){session.removeAttribute("profileReturnId");session.removeAttribute("privacyReturn");}
  response.sendRedirect("http://localhost:3100"+SessionPolicy.returnPath(privacy,target));
 }
 public void backChannel(HttpServletRequest request,HttpServletResponse response,Authentication authentication){
  response.setHeader("Cache-Control","no-store");
  if(!(authentication.getPrincipal() instanceof OidcLogoutToken token)||!sessions.fresh(token)){response.setStatus(400);return;}
  sessions.revoke(token);response.setStatus(200);
 }
}
