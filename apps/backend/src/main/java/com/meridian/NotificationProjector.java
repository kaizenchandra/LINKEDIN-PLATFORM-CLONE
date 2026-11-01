package com.meridian;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
class NotificationProjector {
 private final JdbcTemplate db;
 private final org.springframework.context.ApplicationEventPublisher events;
 NotificationProjector(JdbcTemplate db,org.springframework.context.ApplicationEventPublisher events){this.db=db;this.events=events;}
 @Transactional public void project(ConnectionAccepted event){
  event.validate();
  if(db.update("insert into processed_event(id) values(?) on conflict do nothing",event.eventId())==0)return;
  // This first projection belongs to the same modular application; source rows remain authoritative.
  int inserted=db.update("insert into notification(id,connection_id,recipient,actor) select ?,id,sender,recipient from connection_request c where id=? and state='ACCEPTED' and coalesce((select connections from notification_preference pref where pref.subject=c.sender),true) and not exists(select 1 from member_block b where (b.blocker=c.sender and b.blocked=c.recipient) or (b.blocker=c.recipient and b.blocked=c.sender)) on conflict do nothing",event.eventId(),event.connectionId());
  if(inserted==1){String recipient=db.queryForObject("select recipient from notification where id=?",String.class,event.eventId());events.publishEvent(new RealtimeChanged(java.util.Set.of(recipient)));}
 }

 @Transactional public void project(ApplicationChanged event){
  event.validate();
  // Resolve private audience/status from authoritative audit data, never from a broker payload.
  var source=db.queryForList("select a.applicant,o.owner_subject,h.actor,h.to_status from event_outbox e join application_audit h on h.application_id=e.application_id and h.version=e.application_version join job_application a on a.id=h.application_id join job j on j.id=a.job_id join organization o on o.id=j.organization_id where e.id=? and e.application_id=? and e.application_version=?",event.eventId(),event.applicationId(),event.applicationVersion());
  if(source.isEmpty())return;
  if(db.update("insert into processed_event(id) values(?) on conflict do nothing",event.eventId())==0)return;
  var row=source.getFirst();String recipient=(String)row.get(java.util.Set.of("SUBMITTED","WITHDRAWN").contains(row.get("to_status"))?"owner_subject":"applicant");
  if(!Boolean.TRUE.equals(db.queryForObject("select coalesce((select jobs from notification_preference where subject=?),true)",Boolean.class,recipient)))return;
  int inserted=db.update("insert into notification(id,application_id,application_version,recipient,actor) values(?,?,?,?,?) on conflict do nothing",event.eventId(),event.applicationId(),event.applicationVersion(),recipient,row.get("actor"));
  if(inserted==1)events.publishEvent(new RealtimeChanged(java.util.Set.of(recipient)));
 }
}
