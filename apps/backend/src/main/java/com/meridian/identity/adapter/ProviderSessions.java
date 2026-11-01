package com.meridian.identity.adapter;
import com.meridian.identity.application.ProviderSessionUseCases;
import com.meridian.identity.domain.Sessions.*;
import java.util.List;
import org.springframework.security.oauth2.client.oidc.authentication.logout.OidcLogoutToken;
import org.springframework.security.oauth2.client.oidc.session.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
/** Spring OIDC SPI adapter. Only validated claims, never token values, enter the core. */
@Component
public class ProviderSessions implements OidcSessionRegistry {
 private final ProviderSessionUseCases sessions;private final ClientRegistrationRepository clients;
 public ProviderSessions(ProviderSessionUseCases sessions,ClientRegistrationRepository clients){this.sessions=sessions;this.clients=clients;}
 private Provider provider(){var client=clients.findByRegistrationId("keycloak");return client==null?null:new Provider(client.getProviderDetails().getIssuerUri(),client.getClientId());}
 private Claims claims(OidcLogoutToken token){return new Claims(token.getIssuer().toString(),token.getAudience(),token.getSubject(),token.getSessionId(),token.getIssuedAt(),token.getId());}
 public void saveSessionInformation(OidcSessionInformation information){var token=information.getPrincipal().getIdToken();sessions.bind(information.getSessionId(),new Claims(token.getIssuer().toString(),token.getAudience(),token.getSubject(),token.getClaimAsString("sid"),token.getIssuedAt(),null),provider());}
 // Bindings survive removal to protect against in-flight requests recreating revoked sessions.
 public OidcSessionInformation removeSessionInformation(String sessionId){return null;}
 public Iterable<OidcSessionInformation> removeSessionInformation(OidcLogoutToken token){revoke(token);return List.of();}
 public boolean revoked(String sessionId){return sessions.revoked(sessionId);}
 public void revoke(OidcLogoutToken token){sessions.revoke(claims(token),provider());}
 @Scheduled(fixedDelay=3600000) void cleanup(){sessions.cleanup();}
 public boolean fresh(OidcLogoutToken token){return sessions.fresh(claims(token));}
}
