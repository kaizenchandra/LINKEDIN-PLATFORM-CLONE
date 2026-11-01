package com.meridian.searchhistory.adapter;
import com.meridian.searchhistory.application.SearchHistoryUseCases;
import com.meridian.searchhistory.domain.SearchHistory.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/search-history")
public class SearchHistoryController {
 record Preference(@NotNull Boolean enabled,@NotNull @Min(0) Long version){}
 record Write(@NotNull Kind kind,@NotBlank @Size(min=2,max=80) String query,@NotNull @Size(max=120) String location,@NotNull @Min(0) Long version){}
 private final SearchHistoryUseCases history;
 public SearchHistoryController(SearchHistoryUseCases history){this.history=history;}
 private ResponseEntity<State> response(Supplier<State> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping ResponseEntity<State> get(@AuthenticationPrincipal OidcUser user){return response(()->history.get(user.getSubject()));}
 @PutMapping("/preference") ResponseEntity<State> preference(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Preference desired){return response(()->history.preference(user.getSubject(),desired.enabled(),desired.version()));}
 @PutMapping("/entries") ResponseEntity<State> remember(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Write write){return response(()->history.remember(user.getSubject(),write.kind(),write.query(),write.location(),write.version()));}
 @DeleteMapping ResponseEntity<State> clear(@AuthenticationPrincipal OidcUser user,@RequestParam long version){return response(()->history.clear(user.getSubject(),version));}
 @DeleteMapping("/entries/{id}") ResponseEntity<State> remove(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){return response(()->history.remove(user.getSubject(),id,version));}
}
