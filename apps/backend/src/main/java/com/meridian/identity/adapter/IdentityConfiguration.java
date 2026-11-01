package com.meridian.identity.adapter;
import com.meridian.identity.application.*;
import com.meridian.identity.domain.SessionPolicy;
import com.meridian.shared.application.Transactions;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration
public class IdentityConfiguration {
 @Bean SessionPolicy sessionPolicy(@Value("${meridian.security.session-maximum:12h}") Duration maximum){return new SessionPolicy(maximum,Clock.systemUTC());}
 @Bean ProviderSessionUseCases providerSessionUseCases(ProviderSessionStore store,Transactions transactions,SessionPolicy policy){return new ProviderSessionService(store,transactions,policy);}
 @Bean SessionUseCases sessionUseCases(SessionStore store,ProviderSessionUseCases providers,Transactions transactions,SessionPolicy policy){return new SessionService(store,providers,transactions,policy);}
}
