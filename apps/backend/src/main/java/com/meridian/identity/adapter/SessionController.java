package com.meridian.identity.adapter;
import com.meridian.identity.application.SessionUseCases;
import com.meridian.identity.domain.Sessions.SessionView;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/sessions")
public class SessionController {
 private final SessionUseCases sessions;
 public SessionController(SessionUseCases sessions){this.sessions=sessions;}
 @GetMapping ResponseEntity<List<SessionView>> list(@AuthenticationPrincipal OidcUser user,HttpServletRequest request){String current=request.getSession(false)==null?"":request.getSession(false).getId();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(sessions.list(user.getSubject(),current));}
 @DeleteMapping("/{id}") ResponseEntity<Void> revoke(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){sessions.revoke(user.getSubject(),id);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
 @DeleteMapping ResponseEntity<Void> revokeAll(@AuthenticationPrincipal OidcUser user){sessions.revokeAll(user.getSubject());return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
}
