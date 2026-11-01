package com.meridian.identity.application;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
public interface ProviderSessionStore {
 void bind(String sessionId,Claims claims,Provider provider);
 boolean revoked(String sessionId);
 boolean recordLogout(Claims claims,Provider provider);
 int deleteBoundSessions(Claims claims,Provider provider);
 void recordCount(Claims claims,Provider provider,int count);
 void cleanup();
}
