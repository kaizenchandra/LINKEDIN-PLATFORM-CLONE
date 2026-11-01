package com.meridian.realtime.adapter;
import com.meridian.identity.application.SessionUseCases;
import java.util.concurrent.*;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.*;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class RealtimeHandler extends TextWebSocketHandler {
 record Client(WebSocketSession socket,HttpSession session,String subject){}
 private final ConcurrentHashMap<String,Client> clients=new ConcurrentHashMap<>();
 private final ThreadPoolExecutor delivery;private final MeterRegistry metrics;private final SessionUseCases sessions;
 public RealtimeHandler(MeterRegistry metrics,SessionUseCases sessions){
  this.sessions=sessions;
  this.metrics=metrics;
  delivery=new ThreadPoolExecutor(2,2,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(256),r->{var thread=new Thread(r,"realtime-delivery");thread.setDaemon(true);return thread;},(r,e)->metrics.counter("meridian.realtime.dropped").increment());
  metrics.gauge("meridian.realtime.connections",clients,ConcurrentHashMap::size);
 }
 @Override public synchronized void afterConnectionEstablished(WebSocketSession raw)throws Exception{
  String subject=(String)raw.getAttributes().get("subject");var session=(HttpSession)raw.getAttributes().get("httpSession");
  if(subject==null||session==null||clients.size()>=1000||clients.values().stream().filter(c->c.subject().equals(subject)).count()>=5){raw.close(CloseStatus.POLICY_VIOLATION);return;}
  raw.setTextMessageSizeLimit(1024);raw.setBinaryMessageSizeLimit(1024);
  var client=new Client(new ConcurrentWebSocketSessionDecorator(raw,1000,8192),session,subject);
  if(!valid(client)){raw.close(CloseStatus.POLICY_VIOLATION);return;}
  clients.put(raw.getId(),client);delivery.execute(()->refresh(client));
 }
 private boolean valid(Client client){
  try{
   var session=client.session();if(!sessions.active(session.getId(),client.subject()))return false;int limit=session.getMaxInactiveInterval();
   if(limit>0&&System.currentTimeMillis()-session.getLastAccessedTime()>=limit*1000L)return false;
   var context=session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
   if(!(context instanceof SecurityContext security)||security.getAuthentication()==null||!security.getAuthentication().isAuthenticated())return false;
   return security.getAuthentication().getPrincipal() instanceof OidcUser user&&client.subject().equals(user.getSubject());
  }catch(IllegalStateException|org.springframework.dao.DataAccessException invalidated){return false;}
 }
 private void close(Client client){clients.remove(client.socket().getId());try{client.socket().close(CloseStatus.POLICY_VIOLATION);}catch(Exception ignored){}}
 private void refresh(Client client){
  if(!valid(client)){close(client);return;}
  try{if(client.socket().isOpen()){client.socket().sendMessage(new TextMessage("{\"version\":1,\"type\":\"refresh\"}"));metrics.counter("meridian.realtime.signals").increment();}}
  catch(Exception failure){close(client);}
 }
 @TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT, fallbackExecution=false)
 public void changed(RealtimeChanged event){delivery.execute(()->clients.values().stream().filter(c->event.subjects().contains(c.subject())).forEach(this::refresh));}
 @Scheduled(fixedDelay=5000)
 public void validateSessions(){for(var client:clients.values())if(!valid(client))close(client);}
 @Override protected void handleTextMessage(WebSocketSession session,TextMessage message)throws Exception{session.close(CloseStatus.NOT_ACCEPTABLE);}
 @Override public void handleTransportError(WebSocketSession session,Throwable exception)throws Exception{clients.remove(session.getId());session.close();}
 @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){clients.remove(session.getId());}
 @PreDestroy public void stop(){delivery.shutdownNow();for(var client:clients.values())close(client);}
}
