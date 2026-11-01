package com.meridian.applications.adapter;
import java.util.*;
import com.meridian.applications.application.ApplicationStatusStore;
import com.meridian.applications.domain.ApplicationStatus;
import com.meridian.applications.domain.Applications.*;
import com.meridian.organizations.adapter.OrganizationRecruitingSql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcApplicationStatusStore implements ApplicationStatusStore {
 private final JdbcTemplate db;private final JdbcApplicationReadStore reads;
 public JdbcApplicationStatusStore(JdbcTemplate db,JdbcApplicationReadStore reads){this.db=db;this.reads=reads;}
 public Optional<UUID> organization(UUID id,String subject){return db.queryForList("select j.organization_id from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id where a.id=? and (a.applicant=? or "+OrganizationRecruitingSql.SQL+")",UUID.class,id,subject,subject).stream().findFirst();}
 public boolean lockOrganization(UUID organization){return !db.queryForList("select owner_subject from organization where id=? for update",String.class,organization).isEmpty();}
 public Optional<ApplicationStatus> lockStatus(UUID id,String subject){return db.query("select a.applicant,a.status,a.version,o.owner_subject from job_application a join job j on j.id=a.job_id join organization o on o.id=j.organization_id where a.id=? and (a.applicant=? or "+OrganizationRecruitingSql.SQL+") for update of a",(r,i)->new ApplicationStatus(r.getString("applicant"),State.valueOf(r.getString("status")),r.getLong("version")),id,subject,subject).stream().findFirst();}
 public void update(UUID id,State target){db.update("update job_application set status=?,version=version+1 where id=?",target.name(),id);}
 public void auditAndOutbox(UUID id,long version,String subject,State before,State after){db.update("insert into application_audit(application_id,version,actor,from_status,to_status) values(?,?,?,?,?)",id,version,subject,before.name(),after.name());db.update("insert into event_outbox(id,application_id,application_version) values(?,?,?)",UUID.randomUUID(),id,version);}
 public Set<String> audience(UUID organization,String applicant){var members=new HashSet<>(db.queryForList("select owner_subject from organization where id=? union select member from recruiter_access where organization_id=? and state='ACTIVE'",String.class,organization,organization));members.add(applicant);return members;}
 public Application receipt(UUID id,String subject){return reads.receipt(id,subject);}
}
