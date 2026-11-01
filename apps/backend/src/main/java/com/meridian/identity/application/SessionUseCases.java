package com.meridian.identity.application;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
public interface SessionUseCases {
 List<SessionView> list(String subject,String current);
 boolean active(String sessionId,String subject);
 void revoke(String subject,UUID managementId);
 void revokeAll(String subject);
}
