package com.meridian;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.server.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration @EnableWebSocket @EnableScheduling
class RealtimeConfiguration implements WebSocketConfigurer {
 private final RealtimeHandler handler;private final String origin;
 RealtimeConfiguration(RealtimeHandler handler,@Value("${meridian.realtime.origin:http://localhost:3100}") String origin){this.handler=handler;this.origin=origin;}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
  registry.addHandler(handler,"/api/v1/realtime").setAllowedOrigins(origin).addInterceptors(new HandshakeInterceptor(){
   public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler ws,Map<String,Object> attributes){
    if(!origin.equals(request.getHeaders().getOrigin()))return false;
    if(!(request instanceof ServletServerHttpRequest servlet)||!(request.getPrincipal() instanceof Authentication auth)||!(auth.getPrincipal() instanceof OidcUser user))return false;
    var session=servlet.getServletRequest().getSession(false);if(session==null)return false;
    attributes.put("httpSession",session);attributes.put("subject",user.getSubject());return true;
   }
   public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler ws,Exception exception){}
  });
 }
}
