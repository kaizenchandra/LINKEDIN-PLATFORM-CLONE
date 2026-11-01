package com.meridian.identity.application;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
public interface SessionStore {
 List<SessionRow> list(String subject,String current,long now,long cutoff);
 boolean active(String sessionId,String subject,long now,long cutoff);
 int revoke(String subject,UUID managementId);
 int revokeAll(String subject);
 void audit(String subject,Revocation action,int count);
}
