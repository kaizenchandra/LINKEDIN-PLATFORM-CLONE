package com.meridian;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1")
class RecruiterAccessController {
 private final JdbcTemplate db;private final org.springframework.context.ApplicationEventPublisher events;
 RecruiterAccessController(JdbcTemplate db,org.springframework.context.ApplicationEventPublisher events){this.db=db;this.events=events;}
 record Invite(@NotNull UUID connectionId){}
 enum Action {ACCEPT,DECLINE,LEAVE,REVOKE}
 record Change(@NotNull Action action,@NotNull @Min(1) Long version){}
 record Access(UUID id,UUID organizationId,String companyName,String memberName,String state,long version,boolean owned){}
 record Page(List<Access> items,String nextCursor){}
 private static final String SELECT="select r.*,o.name as company_name,p.name as member_name,o.owner_subject=? as owned from recruiter_access r join organization o on o.id=r.organization_id join profile p on p.subject=r.member";
 private static Access row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new Access(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getString("company_name"),r.getString("member_name"),r.getString("state"),r.getLong("version"),r.getBoolean("owned"));}
 private ResponseEntity<Access> receipt(UUID id,String subject){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(db.queryForObject(SELECT+" where r.id=? and (r.member=? or o.owner_subject=?)",RecruiterAccessController::row,subject,id,subject,subject));}
 private void audit(UUID id,long version,String subject,String before,String after){db.update("insert into recruiter_access_audit(access_id,version,actor,from_state,to_state) values(?,?,?,?,?)",id,version,subject,before,after);}
 @GetMapping("/recruiter-access") ResponseEntity<Page> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) UUID organization,@RequestParam(required=false) UUID after){
  String filter=" where r.member=?";var args=new ArrayList<Object>(List.of(user.getSubject(),user.getSubject()));
  if(organization!=null){if(!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from organization where id=? and owner_subject=?)",Boolean.class,organization,user.getSubject())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Company owner required");filter=" where r.organization_id=?";args.set(1,organization);}
  if(after!=null){filter+=" and r.id>?";args.add(after);}var rows=db.query(SELECT+filter+" order by r.id limit 21",RecruiterAccessController::row,args.toArray());return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null));
 }
 @PutMapping("/organizations/{organization}/recruiter-invitations/{id}") @Transactional
 ResponseEntity<Access> invite(@AuthenticationPrincipal OidcUser user,@PathVariable UUID organization,@PathVariable UUID id,@Valid @RequestBody Invite input){
  String subject=user.getSubject();if(!RecruitingAccess.lock(db,organization).equals(subject))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Company owner required");
  var existing=db.queryForList("select organization_id,connection_id from recruiter_access where id=?",id);if(!existing.isEmpty()){var old=existing.getFirst();if(!organization.equals(old.get("organization_id"))||!input.connectionId().equals(old.get("connection_id")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Invitation ID conflict");return receipt(id,subject);}
  var connections=db.queryForList("select sender,recipient from connection_request where id=? and (sender=? or recipient=?)",input.connectionId(),subject,subject);if(connections.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Accepted connection required");var connection=connections.getFirst();String member=(String)(subject.equals(connection.get("sender"))?connection.get("recipient"):connection.get("sender"));MemberPairLock.acquire(db,subject,member);
  if(!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from connection_request c where c.id=? and c.state='ACCEPTED' and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=?) or (b.blocker=? and b.blocked=?)))",Boolean.class,input.connectionId(),subject,member,member,subject)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Accepted unblocked connection required");
  if(db.queryForObject("select count(*) from recruiter_access where organization_id=? and state in ('PENDING','ACTIVE')",Integer.class,organization)>=50)throw new ResponseStatusException(HttpStatus.CONFLICT,"Recruiter capacity reached");
  if(db.update("insert into recruiter_access(id,organization_id,member,connection_id) values(?,?,?,?) on conflict do nothing",id,organization,member,input.connectionId())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Pending or active access already exists");
  audit(id,1,subject,null,"PENDING");events.publishEvent(new RealtimeChanged(Set.of(subject,member)));return receipt(id,subject);
 }
 @PutMapping("/recruiter-access/{id}") @Transactional
 ResponseEntity<Access> change(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Change input){
  String subject=user.getSubject();var organizations=db.queryForList("select r.organization_id from recruiter_access r join organization o on o.id=r.organization_id where r.id=? and (r.member=? or o.owner_subject=?)",UUID.class,id,subject,subject);if(organizations.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Access unavailable");String owner=RecruitingAccess.lock(db,organizations.getFirst());
  var row=db.queryForMap("select member,state,version from recruiter_access where id=? for update",id);String member=(String)row.get("member"),state=(String)row.get("state");long version=((Number)row.get("version")).longValue();boolean owned=owner.equals(subject);
  if(owned!=(input.action()==Action.REVOKE))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Action not permitted");
  String next=switch(input.action()){case ACCEPT->state.equals("PENDING")?"ACTIVE":null;case DECLINE->state.equals("PENDING")?"DECLINED":null;case LEAVE->state.equals("ACTIVE")?"LEFT":null;case REVOKE->Set.of("PENDING","ACTIVE").contains(state)?"REVOKED":null;};
  if(next==null||input.version()!=version)throw new ResponseStatusException(HttpStatus.CONFLICT,"Access changed; reload before retrying");
  db.update("update recruiter_access set state=?,version=version+1 where id=?",next,id);audit(id,version+1,subject,state,next);events.publishEvent(new RealtimeChanged(Set.of(owner,member)));return receipt(id,subject);
 }
}
