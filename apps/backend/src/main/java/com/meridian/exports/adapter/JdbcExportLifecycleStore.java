package com.meridian.exports.adapter;
import java.util.*;
import com.meridian.exports.domain.ExportRecord;
import com.meridian.exports.application.ExportLifecycleStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcExportLifecycleStore implements ExportLifecycleStore {
 private final JdbcTemplate db;
 public JdbcExportLifecycleStore(JdbcTemplate db){this.db=db;}
 private static ExportRecord row(java.sql.ResultSet r,int i)throws java.sql.SQLException{var expires=r.getTimestamp("expires_at");return new ExportRecord(r.getObject("id",UUID.class),r.getString("status"),r.getTimestamp("created_at").toInstant(),expires==null?null:expires.toInstant(),r.getString("failure"));}
 public List<ExportRecord> list(String subject){return db.query("select id,status,created_at,expires_at,failure from account_export where subject=? order by created_at desc,id limit 10",JdbcExportLifecycleStore::row,subject);}
 public Optional<ExportRecord> owned(String subject,UUID id){return db.query("select id,status,created_at,expires_at,failure from account_export where id=? and subject=?",JdbcExportLifecycleStore::row,id,subject).stream().findFirst();}
 public boolean lockProfile(String subject){return !db.queryForList("select subject from profile where subject=? for update",String.class,subject).isEmpty();}
 public boolean pending(String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from account_export where subject=? and status in ('QUEUED','PROCESSING'))",Boolean.class,subject));}
 public boolean recent(String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from account_export where subject=? and created_at>now()-interval '10 minutes')",Boolean.class,subject));}
 public boolean insert(String subject,UUID id){return db.update("insert into account_export(id,subject) values(?,?) on conflict do nothing",id,subject)==1;}
 public Optional<byte[]> lockPayload(String subject,UUID id){return db.query("select payload from account_export where id=? and subject=? and status='READY' and expires_at>now() for update",(r,n)->r.getBytes(1),id,subject).stream().findFirst();}
 public boolean cancel(String subject,UUID id){return db.update("update account_export set status='EXPIRED',payload=null,lease_until=null,expires_at=now() where id=? and subject=? and status<>'EXPIRED'",id,subject)>0;}
 public void audit(UUID id,String action){db.update("insert into account_export_audit(export_id,action) values(?,?)",id,action);}
}
