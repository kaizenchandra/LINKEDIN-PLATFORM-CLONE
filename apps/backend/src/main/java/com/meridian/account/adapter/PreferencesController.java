package com.meridian.account.adapter;
import com.meridian.account.application.AccountSettingsUseCases;
import com.meridian.account.domain.AccountSettings.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/preferences")
public class PreferencesController {
 record Update(@NotNull Theme theme,@Min(0) long version){}
 private final AccountSettingsUseCases accounts;
 public PreferencesController(AccountSettingsUseCases accounts){this.accounts=accounts;}
 @GetMapping Preferences get(@AuthenticationPrincipal OidcUser user){return invoke(()->accounts.preferences(user.getSubject()));}
 @PutMapping Preferences update(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Update preferences){return invoke(()->accounts.updatePreferences(user.getSubject(),preferences.theme(),preferences.version()));}
}
