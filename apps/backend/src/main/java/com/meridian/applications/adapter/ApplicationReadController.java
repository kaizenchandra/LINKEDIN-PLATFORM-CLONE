package com.meridian.applications.adapter;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.applications.application.ApplicationReadUseCases;
import com.meridian.applications.domain.Applications.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/applications")
public class ApplicationReadController {
 private final ApplicationReadUseCases applications;
 public ApplicationReadController(ApplicationReadUseCases applications){this.applications=applications;}
 @GetMapping ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(defaultValue="MINE") View view,@RequestParam(required=false) UUID job,@RequestParam(required=false) State status,@RequestParam(required=false) UUID after){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->applications.list(user.getSubject(),view,job,status,after)));}
 @GetMapping("/{id}/history") ResponseEntity<History> history(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->applications.history(user.getSubject(),id)));}
 @GetMapping("/{id}/resume") ResponseEntity<byte[]> resume(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  String body=invoke(()->applications.resume(user.getSubject(),id));
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(new MediaType("text","plain",java.nio.charset.StandardCharsets.UTF_8)).header("Content-Disposition","attachment; filename=resume.txt").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; sandbox").body(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }
}
