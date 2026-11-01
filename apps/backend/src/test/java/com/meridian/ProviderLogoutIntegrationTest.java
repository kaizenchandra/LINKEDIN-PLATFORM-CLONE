package com.meridian;
import com.meridian.identity.adapter.ProviderSessions;
import com.meridian.identity.adapter.SessionAccess;
import com.meridian.identity.adapter.SessionLifetimeFilter;
import com.meridian.identity.domain.SessionPolicy;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"meridian.exports.worker-enabled=false","meridian.events.enabled=false","spring.security.oauth2.client.provider.keycloak.issuer-uri=", "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost/authorize", "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost/token", "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost/jwks", "spring.security.oauth2.client.registration.keycloak.client-secret=test"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @Testcontainers
class ProviderLogoutIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @org.springframework.test.context.bean.override.mockito.MockitoBean ClientRegistrationRepository registrations;
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate db;
 @Autowired ProviderSessions sessions;
 @Autowired SessionAccess sessionAccess;
 @Autowired com.meridian.identity.application.ProviderSessionUseCases providerUseCases;
 static RSAKey key;
 static HttpServer jwks;
 static final String ISSUER="http://localhost/test-issuer",URL="/logout/connect/back-channel/keycloak";
 @BeforeAll static void start()throws Exception{
  key=new RSAKeyGenerator(2048).keyID("fixture").generate();jwks=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  jwks.createContext("/jwks",exchange->{byte[] bytes=new JWKSet(key.toPublicJWK()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);try(var out=exchange.getResponseBody()){out.write(bytes);}});jwks.start();
 }
 @AfterAll static void stop(){if(jwks!=null)jwks.stop(0);}
 @BeforeEach void configure(){
  when(registrations.findByRegistrationId("keycloak")).thenReturn(ClientRegistration.withRegistrationId("keycloak").clientId("meridian").clientSecret("test").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://localhost/callback").scope("openid").issuerUri(ISSUER).authorizationUri(ISSUER+"/authorize").tokenUri(ISSUER+"/token").jwkSetUri("http://127.0.0.1:"+jwks.getAddress().getPort()+"/jwks").build());
 }
 JWTClaimsSet.Builder claims(String subject,String sid){return new JWTClaimsSet.Builder().issuer(ISSUER).audience("meridian").issueTime(new Date()).jwtID(UUID.randomUUID().toString()).subject(subject).claim("sid",sid).claim("events",Map.of("http://schemas.openid.net/event/backchannel-logout",Map.of()));}
 String signed(JWTClaimsSet.Builder claims,RSAKey signer)throws Exception{var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("fixture").type(new JOSEObjectType("logout+jwt")).build(),claims.build());jwt.sign(new RSASSASigner(signer));return jwt.serialize();}
 String binding(String subject,String sid,Instant issued){String id=UUID.randomUUID().toString();db.update("insert into oidc_session_binding(session_id,issuer,client_id,subject,provider_sid,token_issued_at) values(?,?,?,?,?,?)",id,ISSUER,"meridian",subject,sid,Timestamp.from(issued));return id;}
 void persist(String id,String subject){long now=System.currentTimeMillis();db.update("insert into spring_session values(?,?,?,?,?,?,?)",UUID.randomUUID().toString(),id,now,now,1800,now+1800000,subject);}
 boolean active(String id,String subject){return sessionAccess.active(id,subject);}
 @Test void signedProviderLogoutRevokesOnlyItsSessionAndSurvivesLateSessionWrites()throws Exception{
  String subject="sid-owner",id=binding(subject,"one",Instant.now().minusSeconds(5)),other=binding(subject,"two",Instant.now().minusSeconds(5)),outsider=binding("other-member","three",Instant.now());persist(id,subject);persist(other,subject);persist(outsider,"other-member");
  String token=signed(claims(subject,"one"),key);
  mvc.perform(post(URL).param("logout_token",token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  assertFalse(active(id,subject));assertTrue(active(other,subject));assertTrue(active(outsider,"other-member"));
  assertEquals(1,db.queryForObject("select revoked_count from provider_logout_event where subject=?",Integer.class,subject));
  persist(id,subject); // Model a login callback committing after the logout transaction.
  assertFalse(active(id,subject));assertTrue(new ProviderSessions(providerUseCases,registrations).revoked(id));
  mvc.perform(post(URL).param("logout_token",token)).andExpect(status().isOk());
  assertEquals(1,db.queryForObject("select count(*) from provider_logout_event where subject=?",Integer.class,subject));
 }
 @Test void subjectLogoutHonorsIssuedTimeAndBothClaimsMustMatch()throws Exception{
  String subject="subject-owner",old=binding(subject,"old",Instant.now().minusSeconds(30)),newer=binding(subject,"new",Instant.now().plusSeconds(20));persist(old,subject);persist(newer,subject);
  mvc.perform(post(URL).param("logout_token",signed(claims("different","old"),key))).andExpect(status().isOk());assertTrue(active(old,subject));
  mvc.perform(post(URL).param("logout_token",signed(claims(subject,null),key))).andExpect(status().isOk());assertFalse(active(old,subject));assertTrue(active(newer,subject));
 }
 @Test void invalidSignedClaimsAndUntrustedSignaturesCannotRevokeSessions()throws Exception{
  String subject="invalid-owner",id=binding(subject,"invalid-sid",Instant.now().minusSeconds(5));persist(id,subject);
  List<JWTClaimsSet.Builder> invalid=List.of(claims(subject,"invalid-sid").issuer("http://localhost/wrong"),claims(subject,"invalid-sid").audience("wrong-client"),claims(subject,"invalid-sid").claim("events",Map.of()),claims(subject,"invalid-sid").claim("nonce","forbidden"),claims(subject,"invalid-sid").jwtID(null),claims(subject,"invalid-sid").issueTime(Date.from(Instant.now().minusSeconds(600))),claims(subject,"invalid-sid").issueTime(Date.from(Instant.now().plusSeconds(180))),claims(subject,"invalid-sid").expirationTime(Date.from(Instant.now().minusSeconds(180))),claims(null,null));
  for(var claim:invalid){mvc.perform(post(URL).param("logout_token",signed(claim,key))).andExpect(status().isBadRequest());assertTrue(active(id,subject));}
  mvc.perform(post(URL).param("logout_token",signed(claims(subject,"invalid-sid"),new RSAKeyGenerator(2048).generate()))).andExpect(status().isBadRequest());
  mvc.perform(post(URL).param("logout_token","not-a-token")).andExpect(status().isBadRequest());mvc.perform(post(URL)).andExpect(status().isBadRequest());
  assertTrue(active(id,subject));assertEquals(0,db.queryForObject("select count(*) from provider_logout_event where subject=?",Integer.class,subject));
 }
 @Test void revokedInFlightHttpSessionCannotReachProtectedResources()throws Exception{
  String subject="late-http",id=binding(subject,"late-sid",Instant.now().minusSeconds(5));
  mvc.perform(post(URL).param("logout_token",signed(claims(subject,"late-sid"),key))).andExpect(status().isOk());
  var session=new org.springframework.mock.web.MockHttpSession(null,id);
  var request=new org.springframework.mock.web.MockHttpServletRequest();request.setSession(session);
  var response=new org.springframework.mock.web.MockHttpServletResponse();
  var user=new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(List.of(),org.springframework.security.oauth2.core.oidc.OidcIdToken.withTokenValue("fixture").subject(subject).build());
  var context=org.springframework.security.core.context.SecurityContextHolder.getContext();context.setAuthentication(new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(user,List.of(),"keycloak"));
  try{
   new SessionLifetimeFilter(new SessionPolicy(Duration.ofHours(12),java.time.Clock.systemUTC()),sessions).doFilter(request,response,(q,r)->fail("Revoked session reached API"));
   assertEquals(401,response.getStatus());assertTrue(session.isInvalid());assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
  }finally{org.springframework.security.core.context.SecurityContextHolder.clearContext();}
 }
}
