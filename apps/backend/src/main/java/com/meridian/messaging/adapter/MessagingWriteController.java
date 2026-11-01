package com.meridian.messaging.adapter;
import java.util.*;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.messaging.application.MessagingWriteUseCases;
import com.meridian.messaging.domain.Messaging.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/conversations")
public class MessagingWriteController {
 private final MessagingWriteUseCases messages;
 public MessagingWriteController(MessagingWriteUseCases messages){this.messages=messages;}
 record Start(@NotNull UUID memberId){} record Started(UUID id){}
 record PreferenceRequest(@NotNull Boolean starred,@NotNull Boolean archived,@NotNull @Min(0) Long version){}
 record Write(@NotBlank @Size(max=3000) String text){} record Read(@NotNull @Min(0) Long throughSequence){}
 private <T> ResponseEntity<T> response(Supplier<T> work){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(work));}
 @PostMapping ResponseEntity<Started> start(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Start request){return response(()->new Started(messages.start(user.getSubject(),request.memberId())));}
 @PutMapping("/{id}/messages/{messageId}") ResponseEntity<Message> send(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@PathVariable UUID messageId,@Valid @RequestBody Write write){return response(()->messages.send(user.getSubject(),id,messageId,write.text()));}
 @PutMapping("/{id}/preferences") ResponseEntity<Preference> preferences(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody PreferenceRequest desired){return response(()->messages.preferences(user.getSubject(),id,new Preference(desired.starred(),desired.archived(),desired.version())));}
 @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) void read(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Read read){invoke(()->{messages.read(user.getSubject(),id,read.throughSequence());return null;});}
}
