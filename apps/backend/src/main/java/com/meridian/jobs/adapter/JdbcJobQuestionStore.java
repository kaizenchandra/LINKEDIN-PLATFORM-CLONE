package com.meridian.jobs.adapter;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.jobs.application.JobQuestionStore;
import com.meridian.jobs.domain.JobQuestions.*;
import com.meridian.organizations.adapter.OrganizationRecruitingSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcJobQuestionStore implements JobQuestionStore {
 private final JdbcTemplate db;private static final ObjectMapper JSON=new ObjectMapper();
 public JdbcJobQuestionStore(JdbcTemplate db){this.db=db;}
 public Optional<UUID> organization(UUID job){return db.queryForList("select organization_id from job where id=?",UUID.class,job).stream().findFirst();}
 public boolean lockOrganization(UUID organization){return !db.queryForList("select owner_subject from organization where id=? for update",String.class,organization).isEmpty();}
 public boolean canRecruit(UUID organization,String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from organization o where o.id=? and "+OrganizationRecruitingSql.SQL+")",Boolean.class,organization,subject));}
 public void lockJob(UUID job){db.queryForObject("select id from job where id=? for update",UUID.class,job);}
 public Form load(UUID job){return load(db,job);}
 public static Form load(JdbcTemplate db,UUID job){
  var rows=db.query("select version,questions::text from job_question_form where job_id=?",(r,i)->new Form(r.getLong(1),decode(r.getString(2))),job);
  return rows.isEmpty()?new Form(0,List.of()):rows.getFirst();
 }
 private static List<Question> decode(String value){try{return Arrays.asList(JSON.readValue(value,Question[].class));}catch(Exception e){throw new IllegalStateException("Invalid stored question form");}}
 private static String encode(Object value){try{return JSON.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Question serialization failed");}}
 public void save(UUID job,Form current,List<Question> questions){
  if(current.version()==0)db.update("insert into job_question_form(job_id,version,questions) values(?,1,?::jsonb)",job,encode(questions));
  else db.update("update job_question_form set version=version+1,questions=?::jsonb where job_id=?",encode(questions),job);
 }
}
