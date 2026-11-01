package com.meridian.jobs.adapter;
import com.meridian.jobs.application.JobUseCases;
import com.meridian.jobs.domain.Jobs.*;
import com.meridian.jobs.domain.Workplace;
import java.util.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/jobs")
public class JobController {
 private final JobUseCases jobs;
 public JobController(JobUseCases jobs){this.jobs=jobs;}
 record Request(@NotNull UUID organizationId,@NotBlank @Size(max=160) String title,@NotBlank @Size(max=120) String location,@NotBlank @Size(max=10000) String description,@NotNull Workplace workplace,@NotNull Status status,@NotNull @Min(0) Long version){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String location,@RequestParam(defaultValue="OPEN") View view,@RequestParam(required=false) UUID organization,@RequestParam(required=false) Workplace workplace,@RequestParam(required=false) UUID after){return response(()->jobs.list(user.getSubject(),q,location,view,organization,workplace,after));}
 @GetMapping("/{id}") ResponseEntity<Job> get(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return response(()->jobs.get(user.getSubject(),id));}
 @PutMapping("/{id}") ResponseEntity<Job> publish(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Request input){return response(()->jobs.publish(user.getSubject(),id,new Write(input.organizationId(),input.title(),input.location(),input.description(),input.workplace(),input.status(),input.version())));}
 private void marker(OidcUser user,UUID id,Marker marker,boolean enabled){invoke(()->{jobs.marker(user.getSubject(),id,marker,enabled);return null;});}
 @PutMapping("/{id}/dismissed") @ResponseStatus(HttpStatus.NO_CONTENT) void dismiss(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){marker(user,id,Marker.DISMISSED,true);}
 @DeleteMapping("/{id}/dismissed") @ResponseStatus(HttpStatus.NO_CONTENT) void restore(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){marker(user,id,Marker.DISMISSED,false);}
 @PutMapping("/{id}/saved") @ResponseStatus(HttpStatus.NO_CONTENT) void save(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){marker(user,id,Marker.SAVED,true);}
 @DeleteMapping("/{id}/saved") @ResponseStatus(HttpStatus.NO_CONTENT) void unsave(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){marker(user,id,Marker.SAVED,false);}
}
