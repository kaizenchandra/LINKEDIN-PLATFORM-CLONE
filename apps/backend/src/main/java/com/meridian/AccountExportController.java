package com.meridian;
import com.meridian.identity.adapter.RecentAuthentication;

import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/me/exports")
class AccountExportController {
 private final JdbcTemplate db;
 private final RecentAuthentication recentAuthentication;
 AccountExportController(JdbcTemplate db,RecentAuthentication recentAuthentication){this.db=db;this.recentAuthentication=recentAuthentication;}
 record Export(UUID id,String status,Instant createdAt,Instant expiresAt,String failure){}
 private static Export row(java.sql.ResultSet r,int index)throws java.sql.SQLException{
  var expires=r.getTimestamp("expires_at");
  String state=r.getString("status");
  if("READY".equals(state)&&expires.toInstant().isBefore(Instant.now()))state="EXPIRED";
  return new Export(r.getObject("id",UUID.class),state,r.getTimestamp("created_at").toInstant(),expires==null?null:expires.toInstant(),r.getString("failure"));
 }
 private List<Export> owned(String subject,UUID id){return db.query("select id,status,created_at,expires_at,failure from account_export where id=? and subject=?",AccountExportController::row,id,subject);}
 @GetMapping ResponseEntity<List<Export>> list(@AuthenticationPrincipal OidcUser user){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(db.query("select id,status,created_at,expires_at,failure from account_export where subject=? order by created_at desc,id limit 10",AccountExportController::row,user.getSubject()));
 }
 @PutMapping("/{id}") @Transactional
 ResponseEntity<Export> request(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  recentAuthentication.require(user);
  if(db.queryForList("select subject from profile where subject=? for update",String.class,user.getSubject()).isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Initialize your profile first");
  var existing=owned(user.getSubject(),id);if(!existing.isEmpty())return response(existing.getFirst());
  if(Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from account_export where subject=? and status in ('QUEUED','PROCESSING'))",Boolean.class,user.getSubject())))throw new ResponseStatusException(HttpStatus.CONFLICT,"An export is already being prepared");
  if(Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from account_export where subject=? and created_at>now()-interval '10 minutes')",Boolean.class,user.getSubject())))return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After","600").cacheControl(CacheControl.noStore()).build();
  if(db.update("insert into account_export(id,subject) values(?,?) on conflict do nothing",id,user.getSubject())!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Export unavailable");
  db.update("insert into account_export_audit(export_id,action) values(?,'REQUESTED')",id);
  return response(owned(user.getSubject(),id).getFirst());
 }
 private ResponseEntity<Export> response(Export result){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);}
 @GetMapping("/{id}/download") @Transactional
 ResponseEntity<byte[]> download(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  recentAuthentication.require(user);
  var rows=owned(user.getSubject(),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Export unavailable");
  if(rows.getFirst().status().equals("EXPIRED"))throw new ResponseStatusException(HttpStatus.GONE,"Export expired");
  if(!rows.getFirst().status().equals("READY"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Export is not ready");
  var bytes=db.query("select payload from account_export where id=? and subject=? and status='READY' and expires_at>now() for update",(r,n)->r.getBytes(1),id,user.getSubject());
  if(bytes.isEmpty())throw new ResponseStatusException(HttpStatus.GONE,"Export expired");
  db.update("insert into account_export_audit(export_id,action) values(?,'DOWNLOADED')",id);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON)
   .header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"meridian-data-"+id+".json\"").body(bytes.getFirst());
 }
 @DeleteMapping("/{id}") @Transactional ResponseEntity<Void> cancel(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id){
  recentAuthentication.require(user);
  if(owned(user.getSubject(),id).isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Export unavailable");
  if(db.update("update account_export set status='EXPIRED',payload=null,lease_until=null,expires_at=now() where id=? and subject=? and status<>'EXPIRED'",id,user.getSubject())>0)
   db.update("insert into account_export_audit(export_id,action) values(?,'CANCELED')",id);
  return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
 }
}
