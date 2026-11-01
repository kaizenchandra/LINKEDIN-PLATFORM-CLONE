package com.meridian.events.adapter;
import java.util.*;
import com.meridian.events.application.OutboxStore;
import com.meridian.events.domain.OutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcOutboxStore implements OutboxStore {
 private final JdbcTemplate db;
 public JdbcOutboxStore(JdbcTemplate db){this.db=db;}
 public List<OutboxEvent> lockReady(){return db.query("select e.id,e.connection_id,e.application_id,e.application_version from event_outbox e where e.published_at is null and e.available_at<=now() and (e.application_id is null or not exists(select 1 from event_outbox older where older.application_id=e.application_id and older.application_version<e.application_version and older.published_at is null)) order by e.created_at,e.id limit 20 for update of e skip locked",(r,i)->new OutboxEvent(r.getObject("id",UUID.class),r.getObject("connection_id",UUID.class),r.getObject("application_id",UUID.class),r.getObject("application_version",Long.class)));}
 public void published(UUID id){db.update("update event_outbox set published_at=now(),attempts=attempts+1 where id=?",id);}
 public void retry(UUID id){db.update("update event_outbox set attempts=attempts+1,available_at=now()+(least(300,power(2,least(attempts,8)))+random()*3)*interval '1 second' where id=?",id);}
}
