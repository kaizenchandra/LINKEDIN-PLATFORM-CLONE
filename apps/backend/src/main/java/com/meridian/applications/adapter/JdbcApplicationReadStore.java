package com.meridian.applications.adapter;
import java.util.*;
import com.meridian.applications.application.ApplicationReadStore;
import com.meridian.applications.domain.Applications.*;
import com.meridian.organizations.adapter.OrganizationRecruitingSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcApplicationReadStore implements ApplicationReadStore {
 private final JdbcTemplate db;
 public JdbcApplicationReadStore(JdbcTemplate db){this.db=db;}
 private static final String SELECT="select a.*,j.title as job_title,o.name as company_name,o.owner_subject,a.applicant=? as mine,exists(select 1 from application_resume ar where ar.application_id=a.id) as has_resume from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id";
 private static Application row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new Application(r.getObject("id",UUID.class),r.getObject("job_id",UUID.class),r.getString("job_title"),r.getString("company_name"),r.getString("applicant_name"),r.getString("applicant_headline"),r.getString("statement"),State.valueOf(r.getString("status")),r.getLong("version"),r.getBoolean("mine"),r.getTimestamp("created_at").toInstant().toString(),r.getBoolean("has_resume"),r.getLong("question_version"),snapshots(r.getString("answers")));}

 private static List<AnswerSnapshot> snapshots(String value){try{return Arrays.asList(new com.fasterxml.jackson.databind.ObjectMapper().readValue(value,AnswerSnapshot[].class));}catch(Exception e){throw new IllegalStateException("Invalid stored answers");}}
 public Application receipt(UUID id,String subject){return db.queryForObject(SELECT+" where a.id=? and (a.applicant=? or "+OrganizationRecruitingSql.SQL+")",JdbcApplicationReadStore::row,subject,id,subject,subject);}
 public List<Application> list(String subject,View view,UUID job,State status,UUID after){
  String filter=view==View.MINE?" where a.applicant=?":" where "+OrganizationRecruitingSql.SQL;var args=new ArrayList<Object>(List.of(subject,subject));
  if(status!=null){filter+=" and a.status=?";args.add(status.name());}
  if(job!=null){filter+=" and a.job_id=?";args.add(job);}if(after!=null){filter+=" and a.id>?";args.add(after);}
  var rows=db.query(SELECT+filter+" order by a.id limit 21",JdbcApplicationReadStore::row,args.toArray());return rows;
 }
 public List<HistoryRow> history(String subject,UUID id){
  var rows=db.query("select a.version as current_version,h.version,h.from_status,h.to_status,h.created_at,case when h.actor=a.applicant then 'APPLICANT' when h.actor=o.owner_subject then 'COMPANY_OWNER' else 'RECRUITER' end as actor_role from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id join application_audit h on h.application_id=a.id where a.id=? and (a.applicant=? or "+OrganizationRecruitingSql.SQL+") order by h.version",(r,i)->new HistoryRow(r.getLong("current_version"),new Transition(r.getLong("version"),r.getString("actor_role"),r.getString("from_status"),r.getString("to_status"),r.getTimestamp("created_at").toInstant().toString())),id,subject,subject);
  return rows;
 }
 public Optional<String> resume(String subject,UUID id){
  var rows=db.queryForList("select r.body from application_resume r join job_application a on a.id=r.application_id join job j on j.id=a.job_id join organization o on o.id=j.organization_id where a.id=? and (a.applicant=? or "+OrganizationRecruitingSql.SQL+")",String.class,id,subject,subject);
  return rows.stream().findFirst();
 }
}
