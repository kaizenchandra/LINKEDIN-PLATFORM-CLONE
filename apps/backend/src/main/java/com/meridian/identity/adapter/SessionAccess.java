package com.meridian.identity.adapter;
import com.meridian.identity.application.SessionUseCases;
import org.springframework.stereotype.Component;
/** Bridge for realtime connections retaining an older servlet session wrapper. */
@Component
public class SessionAccess {
 private final SessionUseCases sessions;
 public SessionAccess(SessionUseCases sessions){this.sessions=sessions;}
 public boolean active(String sessionId,String subject){return sessions.active(sessionId,subject);}
}
