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
@RestController @RequestMapping("/api/v1/network")
public class NetworkController {
 private final NetworkUseCases network;
 public NetworkController(NetworkUseCases network){this.network=network;}
 record Invitation(@NotNull UUID id,@NotNull UUID recipientId){}
 record Transition(@NotNull Action action,@Min(0) long version){}
 @GetMapping List<Connection> list(@AuthenticationPrincipal OidcUser user){return invoke(()->network.connections(user.getSubject()));}
 @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT) void invite(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Invitation invitation){invoke(()->{network.invite(user.getSubject(),invitation.id(),invitation.recipientId());return null;});}
 @PutMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void transition(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Transition request){invoke(()->{network.transition(user.getSubject(),id,request.action(),request.version());return null;});}
}
