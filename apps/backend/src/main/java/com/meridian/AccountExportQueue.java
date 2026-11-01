package com.meridian;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AccountExportQueue {
 record Work(UUID id,String subject,int attempt){}
 private final JdbcTemplate db;
 AccountExportQueue(JdbcTemplate db){this.db=db;}
 @Transactional public Work claim(){
  db.update("update account_export set status='EXPIRED',payload=null where status='READY' and expires_at<=now()");
  db.update("update account_export set status='FAILED',failure='GENERATION_FAILED',lease_until=null where status='PROCESSING' and lease_until<=now() and attempts>=3");
  var rows=db.query("select id,subject,attempts from account_export where attempts<3 and ((status='QUEUED' and available_at<=now()) or (status='PROCESSING' and lease_until<=now())) order by created_at,id limit 1 for update skip locked",(r,n)->new Work(r.getObject(1,UUID.class),r.getString(2),r.getInt(3)+1));
  if(rows.isEmpty())return null;var work=rows.getFirst();
  db.update("update account_export set status='PROCESSING',attempts=?,lease_until=now()+interval '2 minutes' where id=?",work.attempt(),work.id());return work;
 }
 public boolean complete(Work work,byte[] data){return db.update("update account_export set status='READY',payload=?,expires_at=now()+interval '24 hours',lease_until=null,failure=null where id=? and status='PROCESSING' and attempts=?",data,work.id(),work.attempt())==1;}
 public void fail(Work work,boolean tooLarge){
  boolean terminal=tooLarge||work.attempt()>=3;
  db.update("update account_export set status=?,failure=?,lease_until=null,available_at=now()+interval '30 seconds' where id=? and status='PROCESSING' and attempts=?",terminal?"FAILED":"QUEUED",terminal?(tooLarge?"TOO_LARGE":"GENERATION_FAILED"):null,work.id(),work.attempt());
 }
}
