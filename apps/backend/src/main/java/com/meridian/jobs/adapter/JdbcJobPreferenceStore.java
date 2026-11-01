package com.meridian.jobs.adapter;
import com.meridian.jobs.application.JobPreferenceStore;
import com.meridian.jobs.domain.*;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcJobPreferenceStore implements JobPreferenceStore {
 private final JdbcTemplate db;
 public JdbcJobPreferenceStore(JdbcTemplate db){this.db=db;}
 public Optional<JobPreferences> get(String subject){return db.query("select query,location,workplace,version from job_preference where subject=?",(r,i)->new JobPreferences(r.getString(1),r.getString(2),r.getString(3)==null?null:Workplace.valueOf(r.getString(3)),r.getLong(4)),subject).stream().findFirst();}
 public boolean save(String subject,JobPreferences value){String workplace=value.workplace()==null?null:value.workplace().name();return (value.version()==0?db.update("insert into job_preference(subject,query,location,workplace) select subject,?,?,? from profile where subject=? on conflict do nothing",value.query(),value.location(),workplace,subject):db.update("update job_preference set query=?,location=?,workplace=?,version=version+1 where subject=? and version=?",value.query(),value.location(),workplace,subject,value.version()))==1;}
}
