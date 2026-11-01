package com.meridian.content.adapter;
import com.meridian.content.application.DraftUseCases;
import com.meridian.content.domain.Content.*;
import java.util.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/post-drafts")
public class PostDraftController {
 private final DraftUseCases drafts;
 public PostDraftController(DraftUseCases drafts){this.drafts=drafts;}
 record Write(@NotNull @Size(max=3000) String text,@NotNull Audience audience,@NotNull @Min(0) Long version){}
 record Publish(@NotNull @Min(1) Long version){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping ResponseEntity<DraftPage> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) UUID after){return response(()->drafts.list(user.getSubject(),after));}
 @GetMapping("/{id}") ResponseEntity<Draft> get(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return response(()->drafts.get(user.getSubject(),id));}
 @PutMapping("/{id}") ResponseEntity<Draft> save(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Write write){return response(()->drafts.save(user.getSubject(),id,write.text(),write.audience(),write.version()));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){invoke(()->{drafts.delete(user.getSubject(),id,version);return null;});}
 @PostMapping("/{id}/publish") ResponseEntity<Post> publish(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Publish request){return response(()->drafts.publish(user.getSubject(),id,request.version()));}
}
