package com.meridian.moderation.adapter;
import com.meridian.moderation.application.ModerationUseCases;
import com.meridian.moderation.domain.Moderation.*;
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
public class ModerationController {
 private final ModerationUseCases moderation;
 public ModerationController(ModerationUseCases moderation){this.moderation=moderation;}
 record Write(@NotNull UUID postId,@NotNull Category category,@NotNull @Size(max=2000) String details){}
 record Review(@NotNull Decision decision,@NotBlank @Size(max=1000) String reason,@NotNull @Min(1) Long version,@NotNull @Min(1) Long postVersion){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping("/moderation/access") ResponseEntity<Access> access(@AuthenticationPrincipal OidcUser user){return response(()->moderation.access(user.getSubject()));}
 @GetMapping("/reports") ResponseEntity<Page> reports(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) UUID after){return response(()->moderation.reports(user.getSubject(),after));}
 @PutMapping("/reports/{id}") ResponseEntity<Report> submit(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Write write){return response(()->moderation.submit(user.getSubject(),id,write.postId(),write.category(),write.details()));}
 @GetMapping("/moderation/reports") ResponseEntity<ReviewQueue> queue(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) UUID after){return response(()->moderation.queue(user.getSubject(),after));}
 @PutMapping("/moderation/reports/{id}") ResponseEntity<Report> decide(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Review review){return response(()->moderation.decide(user.getSubject(),id,review.decision(),review.reason(),review.version(),review.postVersion()));}
}
