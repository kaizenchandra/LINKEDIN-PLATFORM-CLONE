package com.meridian.applications.adapter;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.applications.application.ApplicationStatusUseCases;
import com.meridian.applications.domain.Applications.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/applications")
public class ApplicationStatusController {
 private final ApplicationStatusUseCases applications;
 public ApplicationStatusController(ApplicationStatusUseCases applications){this.applications=applications;}
 record Change(@NotNull State status,@NotNull @Min(1) Long version){}
 @PutMapping("/{id}/status") ResponseEntity<Application> change(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Change change){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->applications.change(user.getSubject(),id,change.status(),change.version())));}
}
