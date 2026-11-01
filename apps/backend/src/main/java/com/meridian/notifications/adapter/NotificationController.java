package com.meridian.notifications.adapter;
import com.meridian.notifications.application.NotificationUseCases;
import com.meridian.notifications.domain.Notifications.*;
import java.util.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notifications")
public class NotificationController {
 private final NotificationUseCases notifications;
 public NotificationController(NotificationUseCases notifications){this.notifications=notifications;}
 record ReadAll(@NotNull @Pattern(regexp="[1-9][0-9]{0,18}") String through){}
 record PreferenceWrite(@NotNull Boolean enabled,@NotNull @Min(0) Long version,Boolean jobs){}
 @GetMapping ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before,@RequestParam(defaultValue="ALL") Category category){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->notifications.list(user.getSubject(),before,category)));}
 @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) void mark(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){change(user,id,Change.READ);}
 @DeleteMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) void unread(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){change(user,id,Change.UNREAD);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void dismiss(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){change(user,id,Change.DISMISS);}
 private void change(OidcUser user,UUID id,Change change){invoke(()->{notifications.change(user.getSubject(),id,change);return null;});}
 @PutMapping("/read") @ResponseStatus(HttpStatus.NO_CONTENT) void readAll(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody ReadAll write){invoke(()->{notifications.readAll(user.getSubject(),write.through());return null;});}
 @GetMapping("/preferences") Preference preferences(@AuthenticationPrincipal OidcUser user){return invoke(()->notifications.preferences(user.getSubject()));}
 @PutMapping("/preferences") Preference preferences(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody PreferenceWrite write){return invoke(()->notifications.preferences(user.getSubject(),write.enabled(),write.jobs(),write.version()));}
}
