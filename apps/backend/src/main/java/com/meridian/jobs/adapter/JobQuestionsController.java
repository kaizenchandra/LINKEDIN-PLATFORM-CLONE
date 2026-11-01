package com.meridian.jobs.adapter;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import com.meridian.jobs.application.JobQuestionUseCases;
import com.meridian.jobs.domain.JobQuestions.*;
import static com.meridian.shared.adapter.HttpUseCases.invoke;
@RestController @RequestMapping("/api/v1/jobs/{jobId}/questions")
public class JobQuestionsController {
 private final JobQuestionUseCases questions;
 public JobQuestionsController(JobQuestionUseCases questions){this.questions=questions;}
 record QuestionRequest(@NotNull UUID id,@NotBlank @Size(max=300) String prompt,@NotNull Boolean required){}
 record Request(@NotNull @Min(0) Long version,@NotNull @Size(max=5) List<@NotNull @Valid QuestionRequest> questions){}
 private ResponseEntity<Form> response(Form form){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(form);}
 @GetMapping ResponseEntity<Form> get(@PathVariable UUID jobId){return response(invoke(()->questions.get(jobId)));}
 @PutMapping ResponseEntity<Form> save(@AuthenticationPrincipal OidcUser user,@PathVariable UUID jobId,@Valid @RequestBody Request input){return response(invoke(()->questions.save(user.getSubject(),jobId,new Form(input.version(),input.questions().stream().map(q->new Question(q.id(),q.prompt(),q.required())).toList()))));}
}
