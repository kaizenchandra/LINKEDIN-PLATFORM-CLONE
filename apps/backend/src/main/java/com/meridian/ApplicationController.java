package com.meridian;
import java.util.*;
import com.meridian.ApplicationQuestions.Answer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1")
class ApplicationController {
 private final JdbcTemplate db;private final org.springframework.context.ApplicationEventPublisher events;
 ApplicationController(JdbcTemplate db,org.springframework.context.ApplicationEventPublisher events){this.db=db;this.events=events;}
 enum State {SUBMITTED,REVIEWING,ACCEPTED,REJECTED,WITHDRAWN} enum View {MINE,RECEIVED}
 record Submit(@NotBlank @Size(max=4000) String statement,@Valid ResumeText resume,@Min(0) Long questionVersion,@Size(max=5) List<@NotNull @Valid Answer> answers){}
 record Change(@NotNull State status,@NotNull @Min(1) Long version){}
 record Application(UUID id,UUID jobId,String jobTitle,String companyName,String applicantName,String applicantHeadline,String statement,State status,long version,boolean mine,String createdAt,boolean hasResume,long questionVersion,List<ApplicationQuestions.Snapshot> answers){}
 record Page(List<Application> items,String nextCursor){}
 private static final String SELECT="select a.*,j.title as job_title,o.name as company_name,o.owner_subject,a.applicant=? as mine,exists(select 1 from application_resume ar where ar.application_id=a.id) as has_resume from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id";
 private static Application row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new Application(r.getObject("id",UUID.class),r.getObject("job_id",UUID.class),r.getString("job_title"),r.getString("company_name"),r.getString("applicant_name"),r.getString("applicant_headline"),r.getString("statement"),State.valueOf(r.getString("status")),r.getLong("version"),r.getBoolean("mine"),r.getTimestamp("created_at").toInstant().toString(),r.getBoolean("has_resume"),r.getLong("question_version"),ApplicationQuestions.snapshots(r.getString("answers")));}
 private ResponseEntity<Application> receipt(UUID id,String subject){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(db.queryForObject(SELECT+" where a.id=? and (a.applicant=? or "+RecruitingAccess.SQL+")",ApplicationController::row,subject,id,subject,subject));}
 private void audit(UUID id,long version,String actor,String from,State to){db.update("insert into application_audit(application_id,version,actor,from_status,to_status) values(?,?,?,?,?)",id,version,actor,from,to.name());db.update("insert into event_outbox(id,application_id,application_version) values(?,?,?)",UUID.randomUUID(),id,version);}
 @PutMapping("/jobs/{jobId}/application") @Transactional
 ResponseEntity<Application> apply(@AuthenticationPrincipal OidcUser user,@PathVariable UUID jobId,@Valid @RequestBody Submit input){
  String subject=user.getSubject(),statement=input.statement().strip(),resume=input.resume()==null?null:input.resume().validated();
  var organizations=db.queryForList("select organization_id from job where id=?",UUID.class,jobId);
  if(organizations.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Job unavailable");
  UUID organization=organizations.getFirst();RecruitingAccess.lock(db,organization);
  var jobs=db.queryForList("select j.status,o.owner_subject from job j join organization o on o.id=j.organization_id where j.id=? for update of j",jobId);
  if(jobs.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Job unavailable");
  var existing=db.queryForList("select a.id,a.statement,a.question_version,a.answers,r.body from job_application a left join application_resume r on r.application_id=a.id where a.job_id=? and a.applicant=?",jobId,subject);
  long questionVersion=input.questionVersion()==null?0:input.questionVersion();
  if(!existing.isEmpty()){
   var previous=existing.getFirst();var snapshots=ApplicationQuestions.snapshots(previous.get("answers").toString());
   var form=new ApplicationQuestions.Form(((Number)previous.get("question_version")).longValue(),snapshots.stream().map(a->new ApplicationQuestions.Question(a.questionId(),a.prompt(),a.required())).toList());
   if(questionVersion!=form.version()||!previous.get("statement").equals(statement)||!Objects.equals(previous.get("body"),resume)||!ApplicationQuestions.answer(form,input.answers()).equals(snapshots))throw new ResponseStatusException(HttpStatus.CONFLICT,"Application already submitted; submitted content cannot be replaced");
   return receipt((UUID)previous.get("id"),subject);
  }
  var form=ApplicationQuestions.load(db,jobId);
  if(questionVersion!=form.version())throw new ResponseStatusException(HttpStatus.CONFLICT,"Application questions changed; reload questions before submitting");
  var answers=ApplicationQuestions.answer(form,input.answers());
  if(RecruitingAccess.allowed(db,organization,subject)||!"OPEN".equals(jobs.getFirst().get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Job is closed or belongs to your company");
  var profiles=db.queryForList("select name,headline from profile where subject=?",subject);
  if(profiles.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Initialize profile first");
  UUID id=UUID.randomUUID();var profile=profiles.getFirst();
  db.update("insert into job_application(id,job_id,applicant,applicant_name,applicant_headline,statement,question_version,answers) values(?,?,?,?,?,?,?,?::jsonb)",id,jobId,subject,profile.get("name"),profile.get("headline"),statement,questionVersion,ApplicationQuestions.encode(answers));
  if(resume!=null)db.update("insert into application_resume(application_id,body) values(?,?)",id,resume);
  audit(id,1,subject,null,State.SUBMITTED);events.publishEvent(new RealtimeChanged(RecruitingAccess.audience(db,organization,subject)));return receipt(id,subject);
 }
 @GetMapping("/applications/{id}/resume") ResponseEntity<byte[]> resume(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  var rows=db.queryForList("select r.body from application_resume r join job_application a on a.id=r.application_id join job j on j.id=a.job_id join organization o on o.id=j.organization_id where a.id=? and (a.applicant=? or "+RecruitingAccess.SQL+")",String.class,id,user.getSubject(),user.getSubject());
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume unavailable");
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(new MediaType("text","plain",java.nio.charset.StandardCharsets.UTF_8)).header("Content-Disposition","attachment; filename=resume.txt").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","default-src 'none'; sandbox").body(rows.getFirst().getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }
 @GetMapping("/applications") ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(defaultValue="MINE") View view,@RequestParam(required=false) UUID job,@RequestParam(required=false) State status,@RequestParam(required=false) UUID after){
  String subject=user.getSubject(),filter=view==View.MINE?" where a.applicant=?":" where "+RecruitingAccess.SQL;var args=new ArrayList<Object>(List.of(subject,subject));
  if(status!=null){filter+=" and a.status=?";args.add(status.name());}
  if(job!=null){filter+=" and a.job_id=?";args.add(job);}if(after!=null){filter+=" and a.id>?";args.add(after);}
  var rows=db.query(SELECT+filter+" order by a.id limit 21",ApplicationController::row,args.toArray());return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null));
 }
 record Transition(long version,String actorRole,String fromStatus,String toStatus,String createdAt){}
 record History(UUID applicationId,long version,List<Transition> items){}
 record HistoryRow(long currentVersion,Transition transition){}
 @GetMapping("/applications/{id}/history") ResponseEntity<History> history(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  var rows=db.query("select a.version as current_version,h.version,h.from_status,h.to_status,h.created_at,case when h.actor=a.applicant then 'APPLICANT' when h.actor=o.owner_subject then 'COMPANY_OWNER' else 'RECRUITER' end as actor_role from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id join application_audit h on h.application_id=a.id where a.id=? and (a.applicant=? or "+RecruitingAccess.SQL+") order by h.version",(r,i)->new HistoryRow(r.getLong("current_version"),new Transition(r.getLong("version"),r.getString("actor_role"),r.getString("from_status"),r.getString("to_status"),r.getTimestamp("created_at").toInstant().toString())),id,user.getSubject(),user.getSubject());
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Application unavailable");
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new History(id,rows.getFirst().currentVersion(),rows.stream().map(HistoryRow::transition).toList()));
 }
 @PutMapping("/applications/{id}/status") @Transactional
 ResponseEntity<Application> change(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Change change){
  String subject=user.getSubject();
  var organizations=db.queryForList("select j.organization_id from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id where a.id=? and (a.applicant=? or "+RecruitingAccess.SQL+")",UUID.class,id,subject,subject);
  if(organizations.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Application unavailable");
  UUID organization=organizations.getFirst();RecruitingAccess.lock(db,organization);
  var rows=db.queryForList("select a.applicant,a.status,a.version,o.owner_subject from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id where a.id=? and (a.applicant=? or "+RecruitingAccess.SQL+") for update of a",id,subject,subject);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Application unavailable");var row=rows.getFirst();
  boolean mine=subject.equals(row.get("applicant"));State current=State.valueOf((String)row.get("status"));long version=((Number)row.get("version")).longValue();
  if(mine&&change.status()!=State.WITHDRAWN||!mine&&change.status()==State.WITHDRAWN)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Actor cannot perform this transition");
  boolean allowed=mine?(current==State.SUBMITTED||current==State.REVIEWING):(current==State.SUBMITTED&&(change.status()==State.REVIEWING||change.status()==State.REJECTED)||current==State.REVIEWING&&(change.status()==State.ACCEPTED||change.status()==State.REJECTED));
  if(change.version()!=version||!allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Application changed or transition is unavailable");
  db.update("update job_application set status=?,version=version+1 where id=?",change.status().name(),id);audit(id,version+1,subject,current.name(),change.status());
  events.publishEvent(new RealtimeChanged(RecruitingAccess.audience(db,organization,(String)row.get("applicant"))));return receipt(id,subject);
 }
}
