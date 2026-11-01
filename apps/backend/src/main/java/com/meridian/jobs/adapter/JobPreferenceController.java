package com.meridian.jobs.adapter;
import com.meridian.jobs.application.JobPreferenceUseCases;
import com.meridian.jobs.domain.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/job-preferences")
public class JobPreferenceController {
 private final JobPreferenceUseCases preferences;
 public JobPreferenceController(JobPreferenceUseCases preferences){this.preferences=preferences;}
 record Write(@NotNull @Size(max=80) String query,@NotNull @Size(max=120) String location,Workplace workplace,@NotNull @Min(0) Long version){}
 @GetMapping ResponseEntity<JobPreferences> get(@AuthenticationPrincipal OidcUser user){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->preferences.get(user.getSubject())));}
 @PutMapping ResponseEntity<JobPreferences> save(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Write value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->preferences.save(user.getSubject(),new JobPreferences(value.query(),value.location(),value.workplace(),value.version()))));}
}
