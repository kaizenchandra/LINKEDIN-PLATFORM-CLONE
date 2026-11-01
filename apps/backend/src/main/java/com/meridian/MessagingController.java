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

@RestController @RequestMapping("/api/v1/conversations")
class MessagingController {
 private final JdbcTemplate db;
 private final org.springframework.context.ApplicationEventPublisher events;
 MessagingController(JdbcTemplate db,org.springframework.context.ApplicationEventPublisher events){this.db=db;this.events=events;}
 record Start(@NotNull UUID memberId){}
 record Started(UUID id){}
 record Preference(@NotNull Boolean starred,@NotNull Boolean archived,@NotNull @Min(0) Long version){}
 enum View {INBOX,ARCHIVED,STARRED}
 record Conversation(UUID id,UUID memberId,String name,long unreadCount,boolean canSend,Preference preferences){}
 record ConversationRow(Conversation value,long sequence){}
 record Conversations(List<Conversation> items,String nextCursor){}
 record Write(@NotBlank @Size(max=3000) String text){}
 record Read(@NotNull @Min(0) Long throughSequence){}
 record Message(UUID id,long sequence,String text,boolean owned,boolean read,String createdAt){}
 record Messages(List<Message> items,String nextCursor,long latestSequence,long recipientReadSequence,boolean canSend,UUID memberId,String name,Preference preferences){}
 record Access(String a,String b,long latest,long readA,long readB,Preference prefsA,Preference prefsB){Preference preferences(String subject){return subject.equals(a)?prefsA:prefsB;}String other(String subject){return subject.equals(a)?b:a;}long recipientRead(String sender){return sender.equals(a)?readB:readA;}}
 private static final String UNBLOCKED="not exists(select 1 from member_block b where (b.blocker=c.member_a and b.blocked=c.member_b) or (b.blocker=c.member_b and b.blocked=c.member_a))";
 private static final String CONNECTED="exists(select 1 from connection_request r where r.state='ACCEPTED' and ((r.sender=c.member_a and r.recipient=c.member_b) or (r.sender=c.member_b and r.recipient=c.member_a)))";
 private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Conversation or connection unavailable");}
 private Access access(UUID id,String subject,boolean lock){
  var rows=db.query("select member_a,member_b,last_sequence,read_a,read_b,starred_a,archived_a,preference_version_a,starred_b,archived_b,preference_version_b from direct_conversation c where id=? and ? in(member_a,member_b) and "+UNBLOCKED,(r,i)->new Access(r.getString(1),r.getString(2),r.getLong(3),r.getLong(4),r.getLong(5),new Preference(r.getBoolean(6),r.getBoolean(7),r.getLong(8)),new Preference(r.getBoolean(9),r.getBoolean(10),r.getLong(11))),id,subject);
  if(rows.isEmpty())throw unavailable();
  if(lock){var value=rows.getFirst();MemberPairLock.acquire(db,value.a(),value.b());db.queryForList("select id from direct_conversation where id=? for update",id);return access(id,subject,false);}
  return rows.getFirst();
 }
 private boolean connected(String a,String b){return db.queryForObject("select count(*) from connection_request where state='ACCEPTED' and ((sender=? and recipient=?) or (sender=? and recipient=?))",Integer.class,a,b,b,a)>0;}
 @GetMapping ResponseEntity<Conversations> list(@AuthenticationPrincipal OidcUser user,@RequestParam(required=false) Long before,@RequestParam(defaultValue="INBOX") View view,@RequestParam(defaultValue="") String q){
  if(before!=null&&before<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid cursor");
  String query=q.strip();if(query.length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Search names using at most 120 characters");
  String subject=user.getSubject();
  String field=view==View.STARRED?"starred":"archived";
  String filter=" and "+(view==View.INBOX?"not ":"")+"(case when c.member_a=? then c."+field+"_a else c."+field+"_b end)";
  var rows=db.query("select c.*,p.public_id,p.name,"+CONNECTED+" as can_send,(select count(*) from direct_message m where m.conversation_id=c.id and m.sender<>? and m.sequence>case when c.member_a=? then c.read_a else c.read_b end) as unread from direct_conversation c join profile p on p.subject=case when c.member_a=? then c.member_b else c.member_a end where ? in(c.member_a,c.member_b) and "+UNBLOCKED+filter+" and strpos(lower(p.name),lower(?))>0 and c.sequence<? order by c.sequence desc limit 21",(r,i)->new ConversationRow(new Conversation(r.getObject("id",UUID.class),r.getObject("public_id",UUID.class),r.getString("name"),r.getLong("unread"),r.getBoolean("can_send"),new Preference(r.getBoolean("starred_"+(subject.equals(r.getString("member_a"))?"a":"b")),r.getBoolean("archived_"+(subject.equals(r.getString("member_a"))?"a":"b")),r.getLong("preference_version_"+(subject.equals(r.getString("member_a"))?"a":"b")))),r.getLong("sequence")),subject,subject,subject,subject,subject,query,before==null?Long.MAX_VALUE:before);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Conversations(rows.stream().limit(20).map(ConversationRow::value).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null));
 }
 @PostMapping @Transactional
 ResponseEntity<Started> start(@AuthenticationPrincipal OidcUser user,@Valid @RequestBody Start request){
  String subject=user.getSubject();
  var targets=db.queryForList("select subject from profile where public_id=? and subject<>?",String.class,request.memberId(),subject);if(targets.isEmpty())throw unavailable();
  String target=targets.getFirst();MemberPairLock.acquire(db,subject,target);
  if(!connected(subject,target)||db.queryForObject("select count(*) from member_block where (blocker=? and blocked=?) or (blocker=? and blocked=?)",Integer.class,subject,target,target,subject)>0)throw unavailable();
  UUID id=UUID.randomUUID();
  db.update("insert into direct_conversation(id,member_a,member_b) values(?,least(?,?),greatest(?,?)) on conflict(member_a,member_b) do nothing",id,subject,target,subject,target);
  UUID found=db.queryForObject("select id from direct_conversation where member_a=least(?,?) and member_b=greatest(?,?)",UUID.class,subject,target,subject,target);
  events.publishEvent(new RealtimeChanged(Set.of(subject,target)));
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Started(found));
 }
 private Message message(java.sql.ResultSet r,String subject,Access access)throws java.sql.SQLException{
  String sender=r.getString("sender");long seq=r.getLong("sequence");
  return new Message(r.getObject("id",UUID.class),seq,r.getString("body"),sender.equals(subject),seq<=access.recipientRead(sender),r.getTimestamp("created_at").toInstant().toString());
 }
 @GetMapping("/{id}/messages") ResponseEntity<Messages> history(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@RequestParam(required=false) Long before){
  if(before!=null&&before<1)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid cursor");
  String subject=user.getSubject();var allowed=access(id,subject,false);
  // Bound to the sampled high-water mark so concurrent later sends are recovered on refresh.
  var rows=db.query("select * from direct_message where conversation_id=? and sequence<? and sequence<=? order by sequence desc limit 51",(r,i)->message(r,subject,allowed),id,before==null?Long.MAX_VALUE:before,allowed.latest());
  var items=new ArrayList<>(rows.stream().limit(50).toList());Collections.reverse(items);
  var person=db.queryForMap("select public_id,name from profile where subject=?",allowed.other(subject));
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Messages(items,rows.size()>50?Long.toString(rows.get(49).sequence()):null,allowed.latest(),allowed.recipientRead(subject),connected(allowed.a(),allowed.b()),(UUID)person.get("public_id"),(String)person.get("name"),allowed.preferences(subject)));
 }
 @PutMapping("/{id}/messages/{messageId}") @Transactional
 ResponseEntity<Message> send(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@PathVariable UUID messageId,@Valid @RequestBody Write write){
  String subject=user.getSubject();var allowed=access(id,subject,true);if(!connected(allowed.a(),allowed.b()))throw unavailable();
  String body=write.text().strip();if(body.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Message must not be blank");
  var existing=db.queryForList("select conversation_id,sender,body from direct_message where id=?",messageId);
  if(!existing.isEmpty()){var prior=existing.getFirst();if(!id.equals(prior.get("conversation_id"))||!subject.equals(prior.get("sender"))||!body.equals(prior.get("body")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Message ID already used");}
  else{
   // Cross-conversation ID races must not abort the surrounding transaction.
   int inserted=db.update("insert into direct_message(id,conversation_id,sequence,sender,body) values(?,?,?,?,?) on conflict(id) do nothing",messageId,id,allowed.latest()+1,subject,body);
   if(inserted!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Message ID already used");
   events.publishEvent(new RealtimeChanged(Set.of(allowed.a(),allowed.b())));
   db.update("update direct_conversation set last_sequence=last_sequence+1,preference_version_a=preference_version_a+case when archived_a then 1 else 0 end,preference_version_b=preference_version_b+case when archived_b then 1 else 0 end,archived_a=false,archived_b=false where id=?",id);
  }
  Message saved=db.queryForObject("select * from direct_message where id=?",(r,i)->message(r,subject,allowed),messageId);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(saved);
 }

 @PutMapping("/{id}/preferences") @Transactional
 ResponseEntity<Preference> preferences(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Preference desired){
  String subject=user.getSubject();var allowed=access(id,subject,true);
  String suffix=subject.equals(allowed.a())?"a":"b";
  if(db.update("update direct_conversation set starred_"+suffix+"=?,archived_"+suffix+"=?,preference_version_"+suffix+"=preference_version_"+suffix+"+1 where id=? and preference_version_"+suffix+"=?",desired.starred(),desired.archived(),id,desired.version())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Conversation settings changed. Refresh before retrying.");
  events.publishEvent(new RealtimeChanged(Set.of(subject)));
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(access(id,subject,false).preferences(subject));
 }
 @PutMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 void read(@AuthenticationPrincipal OidcUser user,@PathVariable UUID id,@Valid @RequestBody Read read){
  String subject=user.getSubject();var allowed=access(id,subject,true);
  if(read.throughSequence()>allowed.latest())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Read cursor exceeds conversation");
  String column=subject.equals(allowed.a())?"read_a":"read_b";
  db.update("update direct_conversation set "+column+"=greatest("+column+",?) where id=?",read.throughSequence(),id);
  events.publishEvent(new RealtimeChanged(Set.of(allowed.a(),allowed.b())));
 }
}
