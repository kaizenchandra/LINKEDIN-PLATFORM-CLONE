package com.meridian.jobs.adapter;
import com.meridian.jobs.application.JobStore;
import com.meridian.organizations.adapter.OrganizationRecruitingSql;
import com.meridian.jobs.domain.Jobs.*;
import com.meridian.jobs.domain.Workplace;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcJobStore implements JobStore {
 private final JdbcTemplate db;
 public JdbcJobStore(JdbcTemplate db){this.db=db;}
 private static final String SELECT="select j.*,o.name as company_name,o.owner_subject=? as owned,exists(select 1 from saved_job s where s.job_id=j.id and s.subject=?) as saved,"+OrganizationRecruitingSql.SQL+" as can_manage from job j join organization o on o.id=j.organization_id";
 private static Job row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new Job(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getString("company_name"),r.getString("title"),r.getString("location"),r.getString("description"),Workplace.valueOf(r.getString("workplace")),Status.valueOf(r.getString("status")),r.getLong("version"),r.getBoolean("owned"),r.getBoolean("saved"),r.getBoolean("can_manage"));}
 public List<Job> list(String subject,String query,String place,View view,UUID organization,Workplace workplace,UUID after){
  var args=new ArrayList<Object>(List.of(subject,subject,subject,query,query,place));
  String filter=" where (strpos(lower(j.title),lower(?))>0 or strpos(lower(o.name),lower(?))>0) and strpos(lower(j.location),lower(?))>0";
  switch(view){case DISMISSED->{filter+=" and exists(select 1 from dismissed_job d where d.job_id=j.id and d.subject=?)";args.add(subject);}case PREFERRED->{filter+=" and j.status='OPEN' and exists(select 1 from job_preference pref where pref.subject=? and (strpos(lower(j.title),lower(pref.query))>0 or strpos(lower(o.name),lower(pref.query))>0) and strpos(lower(j.location),lower(pref.location))>0 and (pref.workplace is null or pref.workplace=j.workplace))";args.add(subject);}case OPEN->filter+=" and j.status='OPEN'";case MANAGED->{filter+=" and "+OrganizationRecruitingSql.SQL;args.add(subject);}case SAVED->{filter+=" and exists(select 1 from saved_job s where s.job_id=j.id and s.subject=?)";args.add(subject);}}
  if(view==View.OPEN||view==View.PREFERRED){filter+=" and not exists(select 1 from dismissed_job d where d.job_id=j.id and d.subject=?)";args.add(subject);}
  if(organization!=null){filter+=" and j.organization_id=?";args.add(organization);}if(workplace!=null){filter+=" and j.workplace=?";args.add(workplace.name());}if(after!=null){filter+=" and j.id>?";args.add(after);}
  var rows=db.query(SELECT+filter+" order by j.id limit 21",JdbcJobStore::row,args.toArray());
 return rows;
 }
 public Optional<Job> get(String subject,UUID id){return db.query(SELECT+" where j.id=?",JdbcJobStore::row,subject,subject,subject,id).stream().findFirst();}
 public boolean lockOrganization(UUID organization){return !db.queryForList("select owner_subject from organization where id=? for update",String.class,organization).isEmpty();}
 public boolean recruitingAllowed(String subject,UUID organization){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from organization o where o.id=? and "+OrganizationRecruitingSql.SQL+")",Boolean.class,organization,subject));}
 public boolean insert(UUID id,Write value){return db.update("insert into job(id,organization_id,title,location,description,workplace,status) values(?,?,?,?,?,?,?) on conflict(id) do nothing",id,value.organizationId(),value.title(),value.location(),value.description(),value.workplace().name(),value.status().name())==1;}
 public Optional<UUID> lockJobOrganization(UUID id){return db.queryForList("select organization_id from job where id=? for update",UUID.class,id).stream().findFirst();}
 public boolean update(UUID id,Write value){return db.update("update job set title=?,location=?,description=?,workplace=?,status=?,version=version+1,updated_at=now() where id=? and version=?",value.title(),value.location(),value.description(),value.workplace().name(),value.status().name(),id,value.version())==1;}
 public boolean initialized(String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from profile where subject=?)",Boolean.class,subject));}
 public void marker(String subject,UUID id,Marker marker,boolean enabled){switch(marker){
  case SAVED->{if(enabled)db.update("insert into saved_job(job_id,subject) values(?,?) on conflict do nothing",id,subject);else db.update("delete from saved_job where job_id=? and subject=?",id,subject);}
  case DISMISSED->{if(enabled)db.update("insert into dismissed_job(subject,job_id) values(?,?) on conflict do nothing",subject,id);else db.update("delete from dismissed_job where subject=? and job_id=?",subject,id);}
 }}
}
