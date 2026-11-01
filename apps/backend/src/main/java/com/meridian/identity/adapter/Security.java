package com.meridian.identity.adapter;
import com.meridian.identity.adapter.*;
import com.meridian.identity.domain.SessionPolicy;
import com.meridian.shared.adapter.ApplicationBodyLimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class Security {
 @Bean SecurityFilterChain chain(HttpSecurity http, ClientRegistrationRepository clients, SessionPolicy lifetime, ProviderSessions providerSessions, BrowserAuthenticationHandlers handlers, @org.springframework.beans.factory.annotation.Value("${management.server.port}") int managementPort) throws Exception {
  var authorization = new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
  authorization.setAuthorizationRequestCustomizer(builder -> {
   OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
   // A revoked Meridian session must not silently reappear through an existing SSO cookie.
   builder.additionalParameters(parameters -> parameters.put("prompt","login"));
  });
  var logout = new OidcClientInitiatedLogoutSuccessHandler(clients);
  return http.requestCache(c -> c.disable()).authorizeHttpRequests(a -> a.requestMatchers("/readyz", "/livez").permitAll()
     .requestMatchers(request -> request.getLocalPort()==managementPort && (request.getRequestURI().equals("/actuator/prometheus") || request.getRequestURI().startsWith("/actuator/health"))).permitAll()
     .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/profiles/*","/api/v1/profiles/*/photo","/api/v1/profile-login/*").permitAll()
     .requestMatchers("/actuator/**").denyAll().anyRequest().authenticated())
   .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor((q,s,x) -> s.sendError(401), new org.springframework.security.web.util.matcher.OrRequestMatcher(new AntPathRequestMatcher("/api/**"), new AntPathRequestMatcher("/actuator/**"))))
   .oauth2Login(o -> o.authorizationEndpoint(a -> a.authorizationRequestResolver(new AuthenticationRequests(authorization)))
     .failureHandler(handlers::failure)
     .successHandler(handlers::success))
   .oidcLogout(o -> o.oidcSessionRegistry(providerSessions).backChannel(b -> b.logoutHandler(handlers::backChannel)))
   .logout(l -> l.logoutUrl("/api/v1/logout").logoutSuccessHandler(logout)
     .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
   .addFilterBefore(new SessionLifetimeFilter(lifetime,providerSessions),org.springframework.security.web.access.intercept.AuthorizationFilter.class)
   .addFilterAfter(new ApplicationBodyLimit(),org.springframework.security.web.access.intercept.AuthorizationFilter.class)
   .build();
 }
}
