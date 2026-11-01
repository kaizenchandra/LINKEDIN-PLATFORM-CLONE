package com.meridian.applications.adapter;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.applications.application.ApplicationSubmissionUseCases;
import com.meridian.applications.domain.Submission.*;
import com.meridian.applications.domain.Applications.Application;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/jobs")
public class ApplicationSubmissionController {
 private final ApplicationSubmissionUseCases applications;
 public ApplicationSubmissionController(ApplicationSubmissionUseCases applications){this.applications=applications;}
 record ResumeRequest(@NotBlank @Size(max=120) String fileName,@NotBlank @Size(max=20480) String text){}
 record AnswerRequest(@NotNull UUID questionId,@NotNull @Size(max=1000) String text){}
 record Submit(@NotBlank @Size(max=4000) String statement,@Valid ResumeRequest resume,@Min(0) Long questionVersion,@Size(max=5) List<@NotNull @Valid AnswerRequest> answers){}
 @PutMapping("/{jobId}/application") ResponseEntity<Application> apply(@AuthenticationPrincipal OidcUser user,@PathVariable UUID jobId,@Valid @RequestBody Submit input){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(invoke(()->applications.submit(user.getSubject(),jobId,new Input(input.statement(),input.resume()==null?null:new Resume(input.resume().fileName(),input.resume().text()),input.questionVersion(),input.answers()==null?null:input.answers().stream().map(a->new Answer(a.questionId(),a.text())).toList()))));}
}
