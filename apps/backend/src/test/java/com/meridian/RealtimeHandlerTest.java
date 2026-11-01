package com.meridian;
import com.meridian.realtime.adapter.RealtimeChanged;
import com.meridian.realtime.adapter.RealtimeHandler;
import com.meridian.identity.application.SessionUseCases;

import java.util.Map;
import java.util.Set;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.socket.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class RealtimeHandlerTest {
 SessionUseCases activeSessions(){var sessions=mock(SessionUseCases.class);when(sessions.active(anyString(),anyString())).thenReturn(true);return sessions;}
 record Fixture(WebSocketSession socket, MockHttpSession session) {}
 Fixture client(String subject)throws Exception {
  var session=new MockHttpSession();var user=mock(OidcUser.class);when(user.getSubject()).thenReturn(subject);
  session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
   new SecurityContextImpl(new UsernamePasswordAuthenticationToken(user,null,java.util.List.of())));
  var socket=mock(WebSocketSession.class);when(socket.getId()).thenReturn(subject);
  when(socket.isOpen()).thenReturn(true);when(socket.getAttributes()).thenReturn(Map.of("subject",subject,"httpSession",session));
  return new Fixture(socket,session);
 }
 @Test void signalsContainNoContentAndOnlyReachTheirAudience()throws Exception {
  var handler=new RealtimeHandler(new SimpleMeterRegistry(),activeSessions());
  try {
   var a=client("a");var b=client("b");handler.afterConnectionEstablished(a.socket());handler.afterConnectionEstablished(b.socket());
   verify(a.socket(),timeout(2000)).sendMessage(any());verify(b.socket(),timeout(2000)).sendMessage(any());
   handler.changed(new RealtimeChanged(Set.of("a")));
   verify(a.socket(),timeout(2000).times(2)).sendMessage(argThat(message->message.getPayload().equals("{\"version\":1,\"type\":\"refresh\"}")));
   verify(b.socket(),after(100).times(1)).sendMessage(any());
  } finally {handler.stop();}
 }
 @Test void invalidatedSessionsAreClosedBeforeMoreSignals()throws Exception {
  var metrics=new SimpleMeterRegistry();var handler=new RealtimeHandler(metrics,activeSessions());
  try {
   var a=client("a");handler.afterConnectionEstablished(a.socket());verify(a.socket(),timeout(2000)).sendMessage(any());
   a.session().invalidate();handler.validateSessions();
   verify(a.socket()).close(CloseStatus.POLICY_VIOLATION);
   assertEquals(0,metrics.get("meridian.realtime.connections").gauge().value());
   handler.changed(new RealtimeChanged(Set.of("a")));
   verify(a.socket(),after(100).times(1)).sendMessage(any());
  } finally {handler.stop();}
 }
 @Test void sessionSubjectMustMatchHandshakeSubject()throws Exception {
  var handler=new RealtimeHandler(new SimpleMeterRegistry(),activeSessions());
  try {
   var a=client("a");when(a.socket().getAttributes()).thenReturn(Map.of("subject","other","httpSession",a.session()));
   handler.afterConnectionEstablished(a.socket());verify(a.socket()).close(CloseStatus.POLICY_VIOLATION);verify(a.socket(),never()).sendMessage(any());
  } finally {handler.stop();}
 }
 @Test void remoteRevocationClosesRetainedSocketSession()throws Exception{
  var sessions=activeSessions();var handler=new RealtimeHandler(new SimpleMeterRegistry(),sessions);
  try{var a=client("remote");handler.afterConnectionEstablished(a.socket());verify(a.socket(),timeout(2000)).sendMessage(any());
   when(sessions.active(a.session().getId(),"remote")).thenReturn(false);handler.validateSessions();
   verify(a.socket()).close(CloseStatus.POLICY_VIOLATION);
  }finally{handler.stop();}
 }
 @Test void queuedAudienceCannotBeChangedByPublisher(){
  var subjects=new java.util.HashSet<>(Set.of("owner"));
  var event=new RealtimeChanged(subjects);subjects.clear();subjects.add("outsider");
  assertEquals(Set.of("owner"),event.subjects());
  assertThrows(UnsupportedOperationException.class,()->event.subjects().add("outsider"));
 }
 @Test void unavailableSessionStoreFailsClosed()throws Exception{
  var sessions=activeSessions();var handler=new RealtimeHandler(new SimpleMeterRegistry(),sessions);
  try{var a=client("database-outage");handler.afterConnectionEstablished(a.socket());verify(a.socket(),timeout(2000)).sendMessage(any());
   when(sessions.active(a.session().getId(),"database-outage")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("Unavailable"));
   handler.validateSessions();verify(a.socket()).close(CloseStatus.POLICY_VIOLATION);
   handler.changed(new RealtimeChanged(Set.of("database-outage")));verify(a.socket(),after(100).times(1)).sendMessage(any());
  }finally{handler.stop();}
 }
}
