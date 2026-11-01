package com.meridian.identity.adapter;

import jakarta.servlet.*;
import com.meridian.identity.domain.SessionPolicy;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;

/** A busy browser cannot extend authentication indefinitely through polling. */
public final class SessionLifetimeFilter extends OncePerRequestFilter {
 private final SessionPolicy lifetime;
 private final ProviderSessions providerSessions;
 public SessionLifetimeFilter(SessionPolicy lifetime,ProviderSessions providerSessions){this.lifetime=lifetime;this.providerSessions=providerSessions;}
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  var session=request.getSession(false);
  if(session!=null&&authentication!=null&&authentication.getPrincipal() instanceof OidcUser&&(lifetime.expired(session.getCreationTime())||providerSessions.revoked(session.getId()))){
   session.invalidate();SecurityContextHolder.clearContext();
   response.setHeader("Cache-Control","no-store");response.sendError(401);return;
  }
  chain.doFilter(request,response);
 }
}
