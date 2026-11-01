package com.meridian.notifications.adapter;
import java.util.*;
import com.meridian.notifications.application.NotificationProjectionStore;
import com.meridian.notifications.domain.NotificationSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcNotificationProjectionStore implements NotificationProjectionStore {
 private final JdbcTemplate db;
 public JdbcNotificationProjectionStore(JdbcTemplate db){this.db=db;}
 public boolean claim(UUID event){return db.update("insert into processed_event(id) values(?) on conflict do nothing",event)!=0;}
 public Optional<String> insertConnection(UUID event,UUID connection){
  int inserted=db.update("insert into notification(id,connection_id,recipient,actor) select ?,id,sender,recipient from connection_request c where id=? and state='ACCEPTED' and coalesce((select connections from notification_preference pref where pref.subject=c.sender),true) and not exists(select 1 from member_block b where (b.blocker=c.sender and b.blocked=c.recipient) or (b.blocker=c.recipient and b.blocked=c.sender)) on conflict do nothing",event,connection);
  return inserted==1?Optional.of(db.queryForObject("select recipient from notification where id=?",String.class,event)):Optional.empty();
 }
 public Optional<NotificationSource> source(UUID event,UUID application,long version){return db.query("select a.applicant,o.owner_subject,h.actor,h.to_status from event_outbox e join application_audit h on h.application_id=e.application_id and h.version=e.application_version join job_application a on a.id=h.application_id join job j on j.id=a.job_id join organization o on o.id=j.organization_id where e.id=? and e.application_id=? and e.application_version=?",(r,i)->new NotificationSource(r.getString("applicant"),r.getString("owner_subject"),r.getString("actor"),r.getString("to_status")),event,application,version).stream().findFirst();}
 public boolean jobsEnabled(String recipient){return Boolean.TRUE.equals(db.queryForObject("select coalesce((select jobs from notification_preference where subject=?),true)",Boolean.class,recipient));}
 public boolean insertApplication(UUID event,UUID application,long version,String recipient,String actor){return db.update("insert into notification(id,application_id,application_version,recipient,actor) values(?,?,?,?,?) on conflict do nothing",event,application,version,recipient,actor)==1;}
}
