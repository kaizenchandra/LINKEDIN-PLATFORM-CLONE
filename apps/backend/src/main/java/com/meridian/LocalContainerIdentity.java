package com.meridian;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/** Local Docker networking only; the public issuer remains validated on ID tokens. */
@Configuration
@Profile("local-container")
class LocalContainerIdentity {
 @Bean ClientRegistrationRepository localClients(@Value("${OIDC_SECRET}") String secret) {
  String issuer="http://localhost:28080/realms/meridian";
  String publicProtocol=issuer+"/protocol/openid-connect";
  String internalProtocol="http://keycloak:8080/realms/meridian/protocol/openid-connect";
  var client=ClientRegistration.withRegistrationId("keycloak")
   .clientId("meridian").clientSecret(secret)
   .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
   .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
   .redirectUri("http://localhost:8081/login/oauth2/code/keycloak")
   .scope("openid","profile","email")
   .issuerUri(issuer).authorizationUri(publicProtocol+"/auth")
   .tokenUri(internalProtocol+"/token").jwkSetUri(internalProtocol+"/certs")
   .userInfoUri(internalProtocol+"/userinfo").userNameAttributeName("sub")
   .providerConfigurationMetadata(Map.of("end_session_endpoint",publicProtocol+"/logout"))
   .clientName("Keycloak").build();
  return new InMemoryClientRegistrationRepository(client);
 }
}
