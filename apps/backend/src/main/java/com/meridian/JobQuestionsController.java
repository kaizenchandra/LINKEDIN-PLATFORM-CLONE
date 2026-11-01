package com.meridian;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/jobs/{jobId}/questions")
class JobQuestionsController {
 private final JdbcTemplate db;
 JobQuestionsController(JdbcTemplate db){this.db=db;}
 private UUID organization(UUID job){var ids=db.queryForList("select organization_id from job where id=?",UUID.class,job);if(ids.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Job unavailable");return ids.getFirst();}
 @GetMapping ResponseEntity<ApplicationQuestions.Form> get(@PathVariable UUID jobId){organization(jobId);return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApplicationQuestions.load(db,jobId));}
 @PutMapping @Transactional ResponseEntity<ApplicationQuestions.Form> save(@AuthenticationPrincipal OidcUser user,@PathVariable UUID jobId,@Valid @RequestBody ApplicationQuestions.Form input){
  UUID company=organization(jobId);RecruitingAccess.lock(db,company);
  if(!RecruitingAccess.allowed(db,company,user.getSubject()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Recruiting access required");
  db.queryForObject("select id from job where id=? for update",UUID.class,jobId);
  var current=ApplicationQuestions.load(db,jobId);
  if(!input.version().equals(current.version()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Questions changed; reload before retrying");
  var questions=ApplicationQuestions.normalize(input.questions());
  if(current.version()==0)db.update("insert into job_question_form(job_id,version,questions) values(?,1,?::jsonb)",jobId,ApplicationQuestions.encode(questions));
  else db.update("update job_question_form set version=version+1,questions=?::jsonb where job_id=?",ApplicationQuestions.encode(questions),jobId);
  return get(jobId);
 }
}
