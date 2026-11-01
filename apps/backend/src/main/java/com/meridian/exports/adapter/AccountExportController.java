package com.meridian.exports.adapter;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.identity.adapter.RecentAuthentication;
import com.meridian.exports.application.ExportLifecycleUseCases;
import com.meridian.exports.domain.ExportRecord;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/me/exports")
public class AccountExportController {
 private final ExportLifecycleUseCases exports;private final RecentAuthentication recent;
 public AccountExportController(ExportLifecycleUseCases exports,RecentAuthentication recent){this.exports=exports;this.recent=recent;}
 @GetMapping ResponseEntity<List<ExportRecord>> list(@AuthenticationPrincipal OidcUser user){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->exports.list(user.getSubject())));}
 @PutMapping("/{id}") ResponseEntity<ExportRecord> request(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){recent.require(user);var result=invoke(()->exports.request(user.getSubject(),id));return result.throttled()?ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After","600").cacheControl(CacheControl.noStore()).build():ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result.export());}
 @GetMapping("/{id}/download") ResponseEntity<byte[]> download(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){recent.require(user);var bytes=invoke(()->exports.download(user.getSubject(),id));return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"meridian-data-"+id+".json\"").body(bytes);}
 @DeleteMapping("/{id}") ResponseEntity<Void> cancel(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){recent.require(user);invoke(()->{exports.cancel(user.getSubject(),id);return null;});return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
}
