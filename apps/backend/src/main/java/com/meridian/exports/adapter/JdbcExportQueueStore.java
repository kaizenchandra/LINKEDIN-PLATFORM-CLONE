package com.meridian.exports.adapter;
import java.util.UUID;
import com.meridian.exports.domain.ExportWork;
import com.meridian.exports.application.ExportQueueStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcExportQueueStore implements ExportQueueStore {
 private final JdbcTemplate db;
 public JdbcExportQueueStore(JdbcTemplate db){this.db=db;}
 public void expire(){db.update("update account_export set status='EXPIRED',payload=null where status='READY' and expires_at<=now()");}
 public void failExhausted(){db.update("update account_export set status='FAILED',failure='GENERATION_FAILED',lease_until=null where status='PROCESSING' and lease_until<=now() and attempts>=3");}
 public ExportWork lockNext(){var rows=db.query("select id,subject,attempts from account_export where attempts<3 and ((status='QUEUED' and available_at<=now()) or (status='PROCESSING' and lease_until<=now())) order by created_at,id limit 1 for update skip locked",(r,n)->new ExportWork(r.getObject(1,UUID.class),r.getString(2),r.getInt(3)+1));return rows.isEmpty()?null:rows.getFirst();}
 public void lease(ExportWork work){db.update("update account_export set status='PROCESSING',attempts=?,lease_until=now()+interval '2 minutes' where id=?",work.attempt(),work.id());}
 public boolean complete(ExportWork work,byte[] data){return db.update("update account_export set status='READY',payload=?,expires_at=now()+interval '24 hours',lease_until=null,failure=null where id=? and status='PROCESSING' and attempts=?",data,work.id(),work.attempt())==1;}
 public void fail(ExportWork work,String status,String failure){db.update("update account_export set status=?,failure=?,lease_until=null,available_at=now()+interval '30 seconds' where id=? and status='PROCESSING' and attempts=?",status,failure,work.id(),work.attempt());}
}
