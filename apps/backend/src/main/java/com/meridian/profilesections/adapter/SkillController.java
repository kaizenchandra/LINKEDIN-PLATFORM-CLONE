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
@RestController @RequestMapping("/api/v1/me/skills")
public class SkillController {
 private final ProfileSectionsUseCases sections;
 public SkillController(ProfileSectionsUseCases sections){this.sections=sections;}
 record Update(@NotNull UUID id,@NotBlank @Size(max=80) String name,@Min(0) long version){}

 @GetMapping List<Skill> list(@AuthenticationPrincipal OidcUser user){return invoke(()->sections.skills(user.getSubject()));}
 @PutMapping Skill save(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Update e){return invoke(()->sections.save(user.getSubject(),new Skill(e.id(),e.name(),e.version())));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){invoke(()->{sections.delete(user.getSubject(),Section.SKILL,id,version);return null;});}
}
