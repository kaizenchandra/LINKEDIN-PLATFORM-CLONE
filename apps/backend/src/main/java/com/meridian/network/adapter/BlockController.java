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
@RestController @RequestMapping("/api/v1/blocks")
public class BlockController {
 private final NetworkUseCases network;
 public BlockController(NetworkUseCases network){this.network=network;}
 record Request(@NotNull UUID memberId){}
 @GetMapping List<Block> list(@AuthenticationPrincipal OidcUser user){return invoke(()->network.blocks(user.getSubject()));}
 @PostMapping @ResponseStatus(HttpStatus.NO_CONTENT) void block(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Request request){invoke(()->{network.block(user.getSubject(),request.memberId());return null;});}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void unblock(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){invoke(()->{network.unblock(user.getSubject(),id);return null;});}
}
