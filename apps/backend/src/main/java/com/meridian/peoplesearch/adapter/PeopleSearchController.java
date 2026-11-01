package com.meridian.peoplesearch.adapter;
import com.meridian.peoplesearch.application.PeopleUseCases;
import com.meridian.peoplesearch.domain.People.*;
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
public class PeopleSearchController {
 private final PeopleUseCases people;
 public PeopleSearchController(PeopleUseCases people){this.people=people;}
 record DiscoveryWrite(@NotNull Boolean discoverable,@NotNull @Min(0) Long version){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping("/me/discovery") ResponseEntity<Discovery> preference(@AuthenticationPrincipal OidcUser user){return response(()->people.discovery(user.getSubject()));}
 @PutMapping("/me/discovery") ResponseEntity<Discovery> preference(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody DiscoveryWrite desired){return response(()->people.discovery(user.getSubject(),desired.discoverable(),desired.version()));}
 @GetMapping("/search/people") ResponseEntity<Page> search(@AuthenticationPrincipal OidcUser user,@RequestParam String q,@RequestParam(defaultValue="") String location,@RequestParam(required=false) UUID after){return response(()->people.search(user.getSubject(),q,location,after));}
}
