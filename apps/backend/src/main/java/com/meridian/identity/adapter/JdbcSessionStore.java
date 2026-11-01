package com.meridian.identity.adapter;
import com.meridian.identity.application.SessionStore;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcSessionStore implements SessionStore {
 private final JdbcTemplate db;
 public JdbcSessionStore(JdbcTemplate db){this.db=db;}
 public List<SessionRow> list(String subject,String current,long now,long cutoff){return db.query("select primary_id,creation_time,last_access_time,expiry_time,session_id=? as current from spring_session where principal_name=? and expiry_time>? and creation_time>? order by creation_time desc,primary_id limit 100",(r,n)->new SessionRow(UUID.fromString(r.getString("primary_id")),r.getLong("creation_time"),r.getLong("last_access_time"),r.getLong("expiry_time"),r.getBoolean("current")),current,subject,now,cutoff);}
 public boolean active(String sessionId,String subject,long now,long cutoff){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from spring_session where session_id=? and principal_name=? and expiry_time>? and creation_time>?)",Boolean.class,sessionId,subject,now,cutoff));}
 public int revoke(String subject,UUID id){return db.update("delete from spring_session where primary_id=? and principal_name=?",id.toString(),subject);}
 public int revokeAll(String subject){return db.update("delete from spring_session where principal_name=?",subject);}
 public void audit(String subject,Revocation action,int count){db.update("insert into session_revocation_audit(subject,action,revoked_count) values (?,?,?)",subject,action.name(),count);}
}
