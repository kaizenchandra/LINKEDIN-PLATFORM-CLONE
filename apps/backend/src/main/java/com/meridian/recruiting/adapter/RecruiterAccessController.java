package com.meridian.recruiting.adapter;
import java.util.*;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.recruiting.application.RecruiterUseCases;
import com.meridian.recruiting.domain.RecruiterAccess.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1")
public class RecruiterAccessController {
 private final RecruiterUseCases recruiters;
 public RecruiterAccessController(RecruiterUseCases recruiters){this.recruiters=recruiters;}
 record Invite(@NotNull UUID connectionId){}
 record Change(@NotNull Action action,@NotNull @Min(1) Long version){}
 private <T> ResponseEntity<T> response(Supplier<T> operation){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(operation));}
 @GetMapping("/recruiter-access") ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) UUID organization,@RequestParam(required=false) UUID after){return response(()->recruiters.list(user.getSubject(),organization,after));}
 @PutMapping("/organizations/{organization}/recruiter-invitations/{id}") ResponseEntity<Access> invite(@AuthenticationPrincipal OidcUser user,@PathVariable UUID organization,@PathVariable UUID id,@Valid @RequestBody Invite input){return response(()->recruiters.invite(user.getSubject(),organization,id,input.connectionId()));}
 @PutMapping("/recruiter-access/{id}") ResponseEntity<Access> change(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Change input){return response(()->recruiters.change(user.getSubject(),id,input.action(),input.version()));}
}
