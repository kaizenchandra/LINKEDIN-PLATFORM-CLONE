package com.meridian.network.adapter;
import com.meridian.network.application.NetworkDiscoveryUseCases;
import com.meridian.network.domain.Network.*;
import java.util.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class NetworkDiscoveryController {
 private final NetworkDiscoveryUseCases network;
 public NetworkDiscoveryController(NetworkDiscoveryUseCases network){this.network=network;}
 record SharingRequest(@NotNull Boolean connectionsVisible,@NotNull @Min(0) Long version){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping("/me/network-privacy") ResponseEntity<Sharing> privacy(@AuthenticationPrincipal OidcUser user){return response(()->network.privacy(user.getSubject()));}
 @PutMapping("/me/network-privacy") ResponseEntity<Sharing> privacy(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody SharingRequest desired){return response(()->network.privacy(user.getSubject(),desired.connectionsVisible(),desired.version()));}
 @GetMapping("/network/suggestions") ResponseEntity<Page> suggestions(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) UUID after){return response(()->network.suggestions(user.getSubject(),after));}
 @GetMapping("/network/mutual/{id}") ResponseEntity<Page> mutual(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam(required=false) UUID after){return response(()->network.mutual(user.getSubject(),id,after));}
 @GetMapping("/followers") ResponseEntity<FollowPage> followers(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before){return response(()->network.followers(user.getSubject(),before));}
}
