package com.meridian.account.adapter;
import com.meridian.account.application.AccountSettingsUseCases;
import com.meridian.account.domain.AccountSettings.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController @RequestMapping("/api/v1")
public class VisibilityController {
 private final AccountSettingsUseCases accounts;
 public VisibilityController(AccountSettingsUseCases accounts){this.accounts=accounts;}
 record Update(@NotNull Boolean memberVisible,Boolean publicVisible,@Min(0) long version){}
 @GetMapping("/me/visibility") Visibility get(@AuthenticationPrincipal OidcUser user){return invoke(()->accounts.visibility(user.getSubject()));}
 @PutMapping("/me/visibility") Visibility update(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Update update){return invoke(()->accounts.updateVisibility(user.getSubject(),update.memberVisible(),update.publicVisible(),update.version()));}
 @GetMapping("/profile-login/{id}") void login(@PathVariable UUID id,jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response)throws java.io.IOException {
  request.getSession().setAttribute("profileReturnId",id);
  response.setHeader("Cache-Control","no-store");
  response.sendRedirect("http://localhost:8081/oauth2/authorization/keycloak");
 }

 @GetMapping("/profiles/{id}") ResponseEntity<Summary> view(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->accounts.view(id,user==null?null:user.getSubject())));}
}
