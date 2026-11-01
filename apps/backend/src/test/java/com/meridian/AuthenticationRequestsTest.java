package com.meridian;
import com.meridian.identity.adapter.AuthenticationRequests;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import static org.junit.jupiter.api.Assertions.*;
class AuthenticationRequestsTest {
 AuthenticationRequests resolver(){
  var client=ClientRegistration.withRegistrationId("keycloak").clientId("meridian").clientSecret("test").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://localhost:8081/login/oauth2/code/keycloak").authorizationUri("http://localhost:28080/authorize").tokenUri("http://localhost:28080/token").scope("openid","profile").build();
  var delegate=new DefaultOAuth2AuthorizationRequestResolver(new InMemoryClientRegistrationRepository(client),"/oauth2/authorization");
  delegate.setAuthorizationRequestCustomizer(builder->{OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);builder.additionalParameters(p->p.put("prompt","login"));});return new AuthenticationRequests(delegate);
 }
 @Test void registrationRetainsPkceStateNonceAndFixedCallback(){
  var request=new MockHttpServletRequest("GET","/oauth2/authorization/keycloak");request.setServletPath("/oauth2/authorization/keycloak");request.setParameter("screen","register");request.setParameter("redirect_uri","https://untrusted.example");
  var result=resolver().resolve(request);
  assertEquals("create",result.getAdditionalParameters().get("prompt"));assertEquals("S256",result.getAdditionalParameters().get("code_challenge_method"));assertNotNull(result.getAdditionalParameters().get("code_challenge"));assertNotNull(result.getAdditionalParameters().get("nonce"));assertNotNull(result.getState());assertEquals("http://localhost:8081/login/oauth2/code/keycloak",result.getRedirectUri());
 }
 @Test void privacyReauthenticationRequiresFreshIdTokenAndFixedReturn(){
  var request=new MockHttpServletRequest();request.setParameter("screen","reauth");request.setParameter("returnTo","https://untrusted.example");
  var result=resolver().resolve(request,"keycloak");
  assertEquals(0,result.getAdditionalParameters().get("max_age"));assertEquals("login",result.getAdditionalParameters().get("prompt"));
  assertEquals(true,request.getSession().getAttribute("privacyReturn"));assertNotNull(result.getAdditionalParameters().get("nonce"));assertNotNull(result.getState());
  assertEquals("http://localhost:8081/login/oauth2/code/keycloak",result.getRedirectUri());
 }
 @Test void arbitraryModesNeverDisableInteractiveLogin(){
  for(String mode:new String[]{"login","none","https://untrusted.example"}){var request=new MockHttpServletRequest();request.setParameter("screen",mode);assertEquals("login",resolver().resolve(request,"keycloak").getAdditionalParameters().get("prompt"));}
  assertNull(resolver().resolve(new MockHttpServletRequest("GET","/unrelated")));
 }
}
