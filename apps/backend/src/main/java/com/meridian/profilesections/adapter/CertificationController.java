package com.meridian.profilesections.adapter;
import com.meridian.profilesections.application.ProfileSectionsUseCases;
import com.meridian.profilesections.domain.ProfileSections.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/certifications")
public class CertificationController {
 private final ProfileSectionsUseCases sections;
 public CertificationController(ProfileSectionsUseCases sections){this.sections=sections;}
 record Entry(@NotNull UUID id,@NotBlank @Size(max=120) String name,
  @NotBlank @Size(max=160) String issuer,@NotNull @PastOrPresent LocalDate issueDate,
  LocalDate expiryDate,@NotNull @Size(max=2048) String credentialUrl,@NotNull @Size(max=160) String credentialId,@Min(0) long version){
  @AssertTrue(message="Credential URL must be an absolute HTTPS URL without embedded credentials")
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isCredentialUrlValid(){
   if(credentialUrl==null || credentialUrl.isBlank())return true;
   try {var uri=new java.net.URI(credentialUrl);return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost()!=null && uri.getUserInfo()==null;}
   catch(java.net.URISyntaxException invalid){return false;}
  }
  @AssertTrue(message="End date must not precede start date")
  @com.fasterxml.jackson.annotation.JsonIgnore
  public boolean isDateRangeValid(){return expiryDate==null || issueDate==null || !expiryDate.isBefore(issueDate);}
 }

 @GetMapping List<Certification> list(@AuthenticationPrincipal OidcUser user){return invoke(()->sections.certifications(user.getSubject()));}
 @PutMapping Certification save(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Entry e){return invoke(()->sections.save(user.getSubject(),new Certification(e.id(),e.name(),e.issuer(),e.issueDate(),e.expiryDate(),e.credentialUrl(),e.credentialId(),e.version())));}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 void delete(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam long version){invoke(()->{sections.delete(user.getSubject(),Section.CERTIFICATION,id,version);return null;});}
}
