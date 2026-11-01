package com.meridian.messaging.adapter;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.messaging.application.MessagingReadUseCases;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/conversations")
public class MessagingReadController {
 private final MessagingReadUseCases messages;
 public MessagingReadController(MessagingReadUseCases messages){this.messages=messages;}
 @GetMapping ResponseEntity<Conversations> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before,@RequestParam(defaultValue="INBOX") View view,@RequestParam(defaultValue="") String q){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->messages.list(user.getSubject(),before,view,q)));}
 @GetMapping("/{id}/messages") ResponseEntity<Messages> history(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam(required=false) Long before){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->messages.history(user.getSubject(),id,before)));}
}
