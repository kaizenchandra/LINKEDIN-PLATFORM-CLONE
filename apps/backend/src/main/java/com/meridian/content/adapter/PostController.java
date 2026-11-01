package com.meridian.content.adapter;
import com.meridian.content.application.PostUseCases;
import com.meridian.content.domain.Content.*;
import java.util.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class PostController {
 private final PostUseCases posts;
 public PostController(PostUseCases posts){this.posts=posts;}
 record Write(@NotBlank @Size(max=3000) String text,@NotNull Audience audience,@Min(0) long version){}
 record HiddenWrite(@NotNull Boolean hidden){}
 record ReactionWrite(@NotNull Boolean liked){}
 record SavedWrite(@NotNull Boolean saved){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping("/feed") ResponseEntity<FeedPage> feed(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before,@RequestParam(defaultValue="RECENT") FeedScope scope){return response(()->posts.feed(user.getSubject(),before,scope));}
 @GetMapping("/saved-posts") ResponseEntity<FeedPage> saved(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before){return response(()->posts.saved(user.getSubject(),before));}
 @GetMapping("/search/posts") ResponseEntity<FeedPage> search(@AuthenticationPrincipal OidcUser user,@RequestParam String q,@RequestParam(required=false) Long before){return response(()->posts.search(user.getSubject(),q,before));}
 @GetMapping("/activity/{id}") ResponseEntity<FeedPage> activity(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam(required=false) Long before){return response(()->posts.activity(user.getSubject(),id,before));}
 @GetMapping("/posts/{id}") ResponseEntity<Post> get(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return response(()->posts.get(user.getSubject(),id));}
 @PutMapping("/posts/{id}") ResponseEntity<Post> save(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Write write){return response(()->posts.save(user.getSubject(),id,write.text(),write.audience(),write.version()));}
 @DeleteMapping("/posts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){invoke(()->{posts.delete(user.getSubject(),id,version);return null;});}
 @PutMapping("/posts/{id}/hidden") ResponseEntity<Void> hidden(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody HiddenWrite desired){invoke(()->{posts.hidden(user.getSubject(),id,desired.hidden());return null;});return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
 @PutMapping("/posts/{id}/reaction") ResponseEntity<Post> react(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody ReactionWrite write){return response(()->posts.react(user.getSubject(),id,write.liked()));}
 @PutMapping("/posts/{id}/saved") ResponseEntity<Post> bookmark(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody SavedWrite write){return response(()->posts.bookmark(user.getSubject(),id,write.saved()));}
}
