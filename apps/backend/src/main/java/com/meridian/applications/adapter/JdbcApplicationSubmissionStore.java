package com.meridian.applications.adapter;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.applications.application.ApplicationSubmissionStore;
import com.meridian.applications.domain.Submission.*;
import com.meridian.applications.domain.Applications.*;
import com.meridian.jobs.domain.JobQuestions.Form;
import com.meridian.jobs.adapter.JdbcJobQuestionStore;
import com.meridian.organizations.adapter.OrganizationRecruitingSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcApplicationSubmissionStore implements ApplicationSubmissionStore {
 private final JdbcTemplate db;private final JdbcApplicationReadStore reads;private final JdbcApplicationStatusStore statuses;
 private static final ObjectMapper JSON=new ObjectMapper();
 public JdbcApplicationSubmissionStore(JdbcTemplate db,JdbcApplicationReadStore reads,JdbcApplicationStatusStore statuses){this.db=db;this.reads=reads;this.statuses=statuses;}
 public Optional<UUID> organization(UUID job){return db.queryForList("select organization_id from job where id=?",UUID.class,job).stream().findFirst();}
 public boolean lockOrganization(UUID organization){return statuses.lockOrganization(organization);}
 public Optional<String> lockJob(UUID job){return db.queryForList("select j.status from job j join organization o on o.id=j.organization_id where j.id=? for update of j",String.class,job).stream().findFirst();}
 private static List<AnswerSnapshot> snapshots(String value){try{return Arrays.asList(JSON.readValue(value,AnswerSnapshot[].class));}catch(Exception e){throw new IllegalStateException("Invalid stored answers");}}
 private static String encode(Object value){try{return JSON.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Question serialization failed");}}
 public Optional<Previous> previous(UUID job,String subject){return db.query("select a.id,a.statement,a.question_version,a.answers,r.body from job_application a left join application_resume r on r.application_id=a.id where a.job_id=? and a.applicant=?",(r,i)->new Previous(r.getObject("id",UUID.class),r.getString("statement"),r.getLong("question_version"),snapshots(r.getString("answers")),r.getString("body")),job,subject).stream().findFirst();}
 public Form questions(UUID job){return JdbcJobQuestionStore.load(db,job);}
 public boolean canRecruit(UUID organization,String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from organization o where o.id=? and "+OrganizationRecruitingSql.SQL+")",Boolean.class,organization,subject));}
 public Optional<Profile> profile(String subject){return db.query("select name,headline from profile where subject=?",(r,i)->new Profile(r.getString(1),r.getString(2)),subject).stream().findFirst();}
 public void insert(UUID id,UUID job,String subject,Profile profile,String statement,long version,List<AnswerSnapshot> answers,String resume){
  db.update("insert into job_application(id,job_id,applicant,applicant_name,applicant_headline,statement,question_version,answers) values(?,?,?,?,?,?,?,?::jsonb)",id,job,subject,profile.name(),profile.headline(),statement,version,encode(answers));
  if(resume!=null)db.update("insert into application_resume(application_id,body) values(?,?)",id,resume);
 }
 public void auditAndOutbox(UUID id,String subject){db.update("insert into application_audit(application_id,version,actor,from_status,to_status) values(?,?,?,?,?)",id,1,subject,null,State.SUBMITTED.name());db.update("insert into event_outbox(id,application_id,application_version) values(?,?,?)",UUID.randomUUID(),id,1);}
 public Set<String> audience(UUID organization,String subject){return statuses.audience(organization,subject);}
 public Application receipt(UUID id,String subject){return reads.receipt(id,subject);}
}
