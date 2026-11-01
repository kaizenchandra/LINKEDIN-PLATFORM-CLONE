package com.meridian.identity.application;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
import com.meridian.identity.domain.SessionPolicy;
import com.meridian.shared.application.Transactions;
public final class ProviderSessionService implements ProviderSessionUseCases {
 private final ProviderSessionStore store;private final Transactions transactions;private final SessionPolicy policy;
 public ProviderSessionService(ProviderSessionStore store,Transactions transactions,SessionPolicy policy){this.store=store;this.transactions=transactions;this.policy=policy;}
 public void bind(String sessionId,Claims claims,Provider provider){claims.requireProvider(provider);store.bind(sessionId,claims,provider);}
 public boolean revoked(String sessionId){return store.revoked(sessionId);}
 public void revoke(Claims claims,Provider provider){
  claims.requireProvider(provider);
  transactions.write(()->{if(store.recordLogout(claims,provider)){int count=store.deleteBoundSessions(claims,provider);store.recordCount(claims,provider,count);}return null;});
 }
 public boolean fresh(Claims claims){return policy.freshLogout(claims.issuedAt());}
 public void cleanup(){store.cleanup();}
}
