package com.meridian.notifications.adapter;
import com.meridian.notifications.application.NotificationStore;
import com.meridian.notifications.domain.Notifications.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcNotificationStore implements NotificationStore {
 private final JdbcTemplate db;
 public JdbcNotificationStore(JdbcTemplate db){this.db=db;}
 private static final String FROM=" from notification n left join connection_request c on c.id=n.connection_id join profile p on p.subject=n.actor left join job_application a on a.id=n.application_id left join job j on j.id=a.job_id left join organization o on o.id=j.organization_id left join application_audit h on h.application_id=n.application_id and h.version=n.application_version where n.recipient=? and n.dismissed_at is null and ((n.connection_id is not null and c.state='ACCEPTED' and not exists(select 1 from member_block b where (b.blocker=n.recipient and b.blocked=n.actor) or (b.blocker=n.actor and b.blocked=n.recipient))) or (n.application_id is not null and h.version is not null and n.recipient in(a.applicant,o.owner_subject)))";
 public List<Row> list(String subject,Long before,Category category){
  String filter=switch(category){case ALL->"";case NETWORK->" and n.connection_id is not null";case JOBS->" and n.application_id is not null";};
  var rows=db.query("select n.*,case when n.application_id is null then p.name else o.name end as actor_name,j.id as job_id,j.title as job_title,h.to_status"+FROM+filter+" and n.sequence < ? order by n.sequence desc limit 21",(r,i)->new Row(new Notification(r.getObject("id",UUID.class),r.getString("actor_name"),r.getTimestamp("created_at").toInstant().toString(),r.getTimestamp("read_at")!=null,r.getObject("application_id")==null?"CONNECTION_ACCEPTED":"APPLICATION_UPDATE",r.getObject("application_id",UUID.class),r.getObject("job_id",UUID.class),r.getString("job_title"),r.getString("to_status"),r.getObject("application_version",Long.class)),r.getLong("sequence")),subject,before==null?Long.MAX_VALUE:before);
  return rows;
 }
 public long unreadCount(String subject){return db.queryForObject("select count(*)"+FROM+" and n.read_at is null",Long.class,subject);}
 public Long readThrough(String subject){return db.queryForObject("select max(n.sequence)"+FROM,Long.class,subject);}
 public boolean change(String subject,UUID id,Change change){return switch(change){
  case READ->db.update("update notification set read_at=coalesce(read_at,now()) where id=? and recipient=? and dismissed_at is null",id,subject)==1;
  case UNREAD->db.update("update notification set read_at=null where id=? and recipient=? and dismissed_at is null",id,subject)==1;
  case DISMISS->db.update("update notification set dismissed_at=coalesce(dismissed_at,now()) where id=? and recipient=?",id,subject)==1;
 };}
 public void readAll(String subject,long through){db.update("update notification set read_at=coalesce(read_at,now()) where id in (select n.id"+FROM+" and n.sequence<=?)",subject,through);}
 public Optional<Preference> preferences(String subject){return db.query("select connections,jobs,version from notification_preference where subject=?",(r,i)->new Preference(r.getBoolean(1),r.getBoolean(2),r.getLong(3)),subject).stream().findFirst();}
 public boolean preferences(String subject,boolean enabled,Boolean jobs,long version){return (version==0?db.update("insert into notification_preference(subject,connections,jobs) select subject,?,coalesce(?,true) from profile where subject=? on conflict do nothing",enabled,jobs,subject):db.update("update notification_preference set connections=?,jobs=coalesce(?,jobs),version=version+1 where subject=? and version=?",enabled,jobs,subject,version))==1;}
}
