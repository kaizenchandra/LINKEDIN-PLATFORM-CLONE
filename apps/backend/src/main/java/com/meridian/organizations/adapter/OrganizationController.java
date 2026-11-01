package com.meridian.organizations.adapter;
import com.meridian.organizations.application.OrganizationUseCases;
import com.meridian.organizations.domain.Organizations.*;
import java.util.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/organizations")
public class OrganizationController {
 private final OrganizationUseCases organizations;
 public OrganizationController(OrganizationUseCases organizations){this.organizations=organizations;}
 record Request(@NotBlank @Size(max=160) String name,@NotNull @Size(max=120) String industry,@NotNull @Size(max=120) String location,@NotNull @Size(max=3000) String about,@NotNull @Min(0) Long version){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="ALL") View view,@RequestParam(required=false) UUID after){return response(()->organizations.list(user.getSubject(),q,view,after));}
 @GetMapping("/{id}") ResponseEntity<Organization> get(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return response(()->organizations.get(user.getSubject(),id));}
 @PutMapping("/{id}") ResponseEntity<Organization> save(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Request input){return response(()->organizations.save(user.getSubject(),id,new Write(input.name(),input.industry(),input.location(),input.about(),input.version())));}
 @PutMapping("/{id}/follow") @ResponseStatus(HttpStatus.NO_CONTENT) void follow(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){invoke(()->{organizations.follow(user.getSubject(),id);return null;});}
 @DeleteMapping("/{id}/follow") @ResponseStatus(HttpStatus.NO_CONTENT) void unfollow(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){invoke(()->{organizations.unfollow(user.getSubject(),id);return null;});}
}
