package com.meridian.network.adapter;
import com.meridian.network.application.NetworkStore;
import com.meridian.network.domain.Network.*;
import com.meridian.MemberPairLock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcNetworkStore implements NetworkStore {
 private final JdbcTemplate db;
 public JdbcNetworkStore(JdbcTemplate db){this.db=db;}
 public void lockPair(String first,String second){MemberPairLock.acquire(db,first,second);}
 public Optional<String> target(UUID id){return db.queryForList("select subject from profile where public_id=?",String.class,id).stream().findFirst();}
 public List<Connection> connections(String subject){return db.query("select c.id,p.name,c.state,c.recipient=?,c.version from connection_request c join profile p on p.subject=case when c.sender=? then c.recipient else c.sender end where (c.sender=? or c.recipient=?) and c.state in ('PENDING','ACCEPTED') order by c.created_at desc,c.id",(r,n)->new Connection(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getBoolean(4),r.getLong(5)),subject,subject,subject,subject);}
 public boolean blocked(String first,String second){return db.queryForObject("select count(*) from member_block where (blocker=? and blocked=?) or (blocker=? and blocked=?)",Integer.class,first,second,second,first)>0;}
 public boolean invitationExists(UUID id,String sender,String recipient){return db.queryForObject("select count(*) from connection_request where id=? and sender=? and recipient=?",Integer.class,id,sender,recipient)==1;}
 public boolean invite(UUID id,String sender,UUID recipientId){try{return db.update("insert into connection_request(id,sender,recipient) select ?,?,subject from profile where public_id=? and member_visible=true and subject<>? and exists(select 1 from profile where subject=?) on conflict do nothing",id,sender,recipientId,sender,sender)==1;}catch(DuplicateKeyException duplicate){return false;}}
 public Optional<Pair> ownedPair(UUID id,String subject){return db.query("select sender,recipient from connection_request where id=? and ? in(sender,recipient)",(r,n)->new Pair(r.getString(1),r.getString(2)),id,subject).stream().findFirst();}
 private String actor(Action action){return switch(action){case ACCEPT,IGNORE->"recipient=?";case WITHDRAW->"sender=?";case REMOVE->"? in (sender,recipient)";};}
 public boolean transitionCompleted(UUID id,String subject,Action action,long nextVersion){return db.queryForObject("select count(*) from connection_request where id=? and state=? and version=? and "+actor(action),Integer.class,id,action.target(),nextVersion,subject)==1;}
 public boolean transition(UUID id,String subject,Action action,long version){return db.update("update connection_request set state=?,version=version+1 where id=? and state=? and version=? and "+actor(action),action.target(),id,action.source(),version,subject)==1;}
 public void enqueueAccepted(UUID id){db.update("insert into event_outbox(id,connection_id) values(?,?)",UUID.randomUUID(),id);}
 private static final String UNBLOCKED="not exists(select 1 from member_block b where (b.blocker=f.follower and b.blocked=f.followed) or (b.blocker=f.followed and b.blocked=f.follower))";
 public List<FollowRow> following(String subject,Long before){return db.query("select p.public_id,case when p.member_visible then p.name else 'Private member' end,p.member_visible,f.sequence from member_follow f join profile p on p.subject=f.followed where f.follower=? and "+UNBLOCKED+" and f.sequence<? order by f.sequence desc limit 21",(r,i)->new FollowRow(new Member(r.getObject(1,UUID.class),r.getString(2),r.getBoolean(3)),r.getLong(4)),subject,before==null?Long.MAX_VALUE:before);}
 public long followingCount(String subject){return db.queryForObject("select count(*) from member_follow f where f.follower=? and "+UNBLOCKED,Long.class,subject);}
 public boolean followed(String subject,String target){return db.queryForObject("select count(*) from member_follow where follower=? and followed=?",Integer.class,subject,target)>0;}
 public boolean visible(String subject){return Boolean.TRUE.equals(db.queryForObject("select member_visible from profile where subject=?",Boolean.class,subject));}
 public boolean profileExists(String subject){return db.queryForObject("select count(*) from profile where subject=?",Integer.class,subject)>0;}
 public void lockProfileVisibility(String target){db.queryForList("select subject from profile where subject=? for share",target);}
 public void follow(String subject,String target,boolean following){
  if(following)db.update("insert into member_follow(follower,followed) values(?,?) on conflict do nothing",subject,target);
  else db.update("delete from member_follow where follower=? and followed=?",subject,target);
 }
 public List<Block> blocks(String subject){return db.query("select p.public_id,p.name from member_block b join profile p on p.subject=b.blocked where b.blocker=? order by b.created_at desc",(r,n)->new Block(r.getObject(1,UUID.class),r.getString(2)),subject);}
 public Optional<String> blockableTarget(String subject,UUID id){return db.queryForList("select subject from profile p where public_id=? and subject<>? and (member_visible=true or exists(select 1 from connection_request c where (c.sender=? and c.recipient=p.subject) or (c.recipient=? and c.sender=p.subject)) or exists(select 1 from member_follow f where (f.follower=? and f.followed=p.subject) or (f.followed=? and f.follower=p.subject)) or exists(select 1 from member_block b where b.blocker=? and b.blocked=p.subject))",String.class,id,subject,subject,subject,subject,subject,subject).stream().findFirst();}
 public void block(String subject,String target){
  db.update("delete from member_follow where (follower=? and followed=?) or (follower=? and followed=?)",subject,target,target,subject);
  db.update("insert into member_block(blocker,blocked) values(?,?) on conflict do nothing",subject,target);
  db.update("update connection_request set state=case when state='ACCEPTED' then 'REMOVED' else 'IGNORED' end,version=version+1 where ((sender=? and recipient=?) or (sender=? and recipient=?)) and state in ('PENDING','ACCEPTED')",subject,target,target,subject);
 }
 public void unblock(String subject,String target){db.update("delete from member_block where blocker=? and blocked=?",subject,target);}
}
