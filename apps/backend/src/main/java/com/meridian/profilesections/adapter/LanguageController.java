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
@RestController @RequestMapping("/api/v1/me/languages")
public class LanguageController {
 private final ProfileSectionsUseCases sections;
 public LanguageController(ProfileSectionsUseCases sections){this.sections=sections;}
 record Update(@NotNull UUID id,@NotBlank @Size(max=80) String name,@NotNull Proficiency proficiency,@Min(0) long version){}

 @GetMapping List<Language> list(@AuthenticationPrincipal OidcUser user){return invoke(()->sections.languages(user.getSubject()));}
 @PutMapping Language save(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Update e){return invoke(()->sections.save(user.getSubject(),new Language(e.id(),e.name(),e.proficiency(),e.version())));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){invoke(()->{sections.delete(user.getSubject(),Section.LANGUAGE,id,version);return null;});}
}
