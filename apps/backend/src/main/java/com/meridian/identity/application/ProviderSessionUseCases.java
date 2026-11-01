package com.meridian.identity.application;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
public interface ProviderSessionUseCases {
 void bind(String sessionId,Claims claims,Provider provider);
 boolean revoked(String sessionId);
 void revoke(Claims claims,Provider provider);
 boolean fresh(Claims claims);
 void cleanup();
}
