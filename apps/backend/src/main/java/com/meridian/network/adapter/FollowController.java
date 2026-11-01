package com.meridian.network.adapter;
import com.meridian.network.application.NetworkUseCases;
import com.meridian.network.domain.Network.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/following")
public class FollowController {
 private final NetworkUseCases network;
 public FollowController(NetworkUseCases network){this.network=network;}
 record State(@NotNull Boolean following){}
 @GetMapping ResponseEntity<FollowPage> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->network.following(user.getSubject(),before)));}
 @GetMapping("/{id}") ResponseEntity<FollowState> state(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->network.followState(user.getSubject(),id)));}
 @PutMapping("/{id}") FollowState set(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody State desired){return invoke(()->network.follow(user.getSubject(),id,desired.following()));}
}
