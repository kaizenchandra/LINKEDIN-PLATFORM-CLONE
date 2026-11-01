package com.meridian.content.adapter;
import com.meridian.content.application.CommentUseCases;
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
@RestController @RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {
 private final CommentUseCases comments;
 public CommentController(CommentUseCases comments){this.comments=comments;}
 record Write(@NotBlank @Size(max=2000) String text,UUID parentId){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping ResponseEntity<CommentPage> list(@AuthenticationPrincipal OidcUser user,@PathVariable UUID postId,@RequestParam(required=false) Long before,@RequestParam(required=false) UUID parentId){return response(()->comments.list(user.getSubject(),postId,before,parentId));}
 @PutMapping("/{id}") ResponseEntity<Comment> create(@AuthenticationPrincipal OidcUser user,@PathVariable UUID postId,@PathVariable UUID id,@Valid @RequestBody Write write){return response(()->comments.create(user.getSubject(),postId,id,write.text(),write.parentId()));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID postId,@PathVariable UUID id){invoke(()->{comments.delete(user.getSubject(),postId,id);return null;});}
}
