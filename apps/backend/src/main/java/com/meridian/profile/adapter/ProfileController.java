package com.meridian.profile.adapter;
import com.meridian.profile.application.ProfileUseCases;
import com.meridian.profile.domain.Profile;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/v1")
public class ProfileController {
 private final ProfileUseCases profiles;
 public ProfileController(ProfileUseCases profiles){this.profiles=profiles;}
 record Update(@NotBlank @Size(max=120) String name,@NotNull @Size(max=220) String headline,@NotNull @Size(max=120) String location,@NotNull @Size(max=2600) String about,@Min(0) long version){}
 @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"header",token.getHeaderName());}
 @GetMapping("/me") Profile me(@AuthenticationPrincipal OidcUser user){return invoke(()->profiles.me(user.getSubject(),user.getFullName()));}
 @PutMapping("/me") Profile update(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Update p){return invoke(()->profiles.update(user.getSubject(),new Profile(p.name(),p.headline(),p.location(),p.about(),p.version())));}
}
