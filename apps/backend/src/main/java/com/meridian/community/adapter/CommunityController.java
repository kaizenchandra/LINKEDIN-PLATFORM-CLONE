package com.meridian.community.adapter;
import com.meridian.community.application.*;
import com.meridian.community.domain.Community.*;
import com.meridian.shared.application.Transactions;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/community")
public class CommunityController {
 private final CommunityService service;
 public CommunityController(CommunityService service){this.service=service;}
 @GetMapping("/catch-up") ResponseEntity<List<Update>> catchUp(@AuthenticationPrincipal OidcUser user){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->service.catchUp(user.getSubject())));}
 @GetMapping ResponseEntity<List<Entry>> list(@AuthenticationPrincipal OidcUser user,@RequestParam Kind kind){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->service.list(user.getSubject(),kind)));}
 @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT) void create(@AuthenticationPrincipal OidcUser user,@RequestBody Draft draft){invoke(()->{service.create(user.getSubject(),draft);return null;});}
 record Membership(boolean joined){}
 @PutMapping("/{id}/membership") @ResponseStatus(HttpStatus.NO_CONTENT) void membership(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestBody Membership request){invoke(()->{service.membership(user.getSubject(),id,request.joined());return null;});}
 @Configuration static class Wiring {@Bean CommunityService communityService(CommunityStore store,Transactions transactions){return new CommunityService(store,transactions);}}
}
