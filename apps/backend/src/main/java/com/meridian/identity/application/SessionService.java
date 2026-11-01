package com.meridian.identity.application;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
import com.meridian.identity.domain.SessionPolicy;
import com.meridian.shared.application.Transactions;
import java.time.Instant;
public final class SessionService implements SessionUseCases {
 private final SessionStore store;private final ProviderSessionUseCases providers;private final Transactions transactions;private final SessionPolicy policy;
 public SessionService(SessionStore store,ProviderSessionUseCases providers,Transactions transactions,SessionPolicy policy){this.store=store;this.providers=providers;this.transactions=transactions;this.policy=policy;}
 public List<SessionView> list(String subject,String current){return store.list(subject,current,policy.now(),policy.cutoff()).stream().map(r->new SessionView(r.id(),Instant.ofEpochMilli(r.creation()),Instant.ofEpochMilli(r.lastAccess()),Instant.ofEpochMilli(Math.min(r.expiry(),policy.deadline(r.creation()))),r.current())).toList();}
 public boolean active(String sessionId,String subject){return !providers.revoked(sessionId)&&store.active(sessionId,subject,policy.now(),policy.cutoff());}
 public void revoke(String subject,UUID id){transactions.write(()->{int count=store.revoke(subject,id);if(count>0)store.audit(subject,Revocation.REVOKE_ONE,count);return null;});}
 public void revokeAll(String subject){transactions.write(()->{int count=store.revokeAll(subject);if(count>0)store.audit(subject,Revocation.REVOKE_ALL,count);return null;});}
}
