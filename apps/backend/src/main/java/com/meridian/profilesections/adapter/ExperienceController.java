package com.meridian.profilesections.adapter;
import com.meridian.profilesections.application.ProfileSectionsUseCases;
import com.meridian.profilesections.domain.ProfileSections.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/experience")
public class ExperienceController {
 private final ProfileSectionsUseCases sections;
 public ExperienceController(ProfileSectionsUseCases sections){this.sections=sections;}
 record Entry(@NotNull UUID id,@NotBlank @Size(max=120) String title,
  @NotBlank @Size(max=160) String organization,@NotNull @PastOrPresent LocalDate startDate,
  LocalDate endDate,@NotNull @Size(max=2600) String description,@Min(0) long version){
  @AssertTrue(message="End date must not precede start date")
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isDateRangeValid(){return endDate==null || startDate==null || !endDate.isBefore(startDate);}
 }

 @GetMapping List<Experience> list(@AuthenticationPrincipal OidcUser user){return invoke(()->sections.experience(user.getSubject()));}
 @PutMapping Experience save(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Entry e){return invoke(()->sections.save(user.getSubject(),new Experience(e.id(),e.title(),e.organization(),e.startDate(),e.endDate(),e.description(),e.version())));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){invoke(()->{sections.delete(user.getSubject(),Section.EXPERIENCE,id,version);return null;});}
}
