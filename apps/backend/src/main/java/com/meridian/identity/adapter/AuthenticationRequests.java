package com.meridian.identity.adapter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/** Selects Keycloak's supported registration prompt while preserving generated state, nonce and PKCE. */
public final class AuthenticationRequests implements OAuth2AuthorizationRequestResolver {
 private final OAuth2AuthorizationRequestResolver delegate;
 public AuthenticationRequests(OAuth2AuthorizationRequestResolver delegate){this.delegate=delegate;}
 public OAuth2AuthorizationRequest resolve(HttpServletRequest request){return select(delegate.resolve(request),request);}
 public OAuth2AuthorizationRequest resolve(HttpServletRequest request,String id){return select(delegate.resolve(request,id),request);}
 private OAuth2AuthorizationRequest select(OAuth2AuthorizationRequest original,HttpServletRequest request){
  if(original==null)return null;
  if("reauth".equals(request.getParameter("screen"))){
   request.getSession().setAttribute("privacyReturn",true);
   return OAuth2AuthorizationRequest.from(original).additionalParameters(parameters->{parameters.put("prompt","login");parameters.put("max_age",0);}).build();
  }
  if(!"register".equals(request.getParameter("screen")))return original;
  return OAuth2AuthorizationRequest.from(original).additionalParameters(parameters->parameters.put("prompt","create")).build();
 }
}
