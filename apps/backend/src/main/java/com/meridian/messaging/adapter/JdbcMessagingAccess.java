package com.meridian.messaging.adapter;
import java.util.*;
import com.meridian.shared.adapter.MemberPairLock;
import com.meridian.messaging.domain.Messaging.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcMessagingAccess {
 private final JdbcTemplate db;
 public JdbcMessagingAccess(JdbcTemplate db){this.db=db;}
 public static final String UNBLOCKED="not exists(select 1 from member_block b where (b.blocker=c.member_a and b.blocked=c.member_b) or (b.blocker=c.member_b and b.blocked=c.member_a))";
 public static final String CONNECTED="exists(select 1 from connection_request r where r.state='ACCEPTED' and ((r.sender=c.member_a and r.recipient=c.member_b) or (r.sender=c.member_b and r.recipient=c.member_a)))";
 public Access access(UUID id,String subject,boolean lock){
  var rows=db.query("select member_a,member_b,last_sequence,read_a,read_b,starred_a,archived_a,preference_version_a,starred_b,archived_b,preference_version_b from direct_conversation c where id=? and ? in(member_a,member_b) and "+UNBLOCKED,(r,i)->new Access(r.getString(1),r.getString(2),r.getLong(3),r.getLong(4),r.getLong(5),new Preference(r.getBoolean(6),r.getBoolean(7),r.getLong(8)),new Preference(r.getBoolean(9),r.getBoolean(10),r.getLong(11))),id,subject);
  if(rows.isEmpty())throw new com.meridian.shared.application.UseCaseFailure(com.meridian.shared.application.UseCaseFailure.Reason.NOT_FOUND,"Conversation or connection unavailable");
  if(lock){var value=rows.getFirst();MemberPairLock.acquire(db,value.a(),value.b());db.queryForList("select id from direct_conversation where id=? for update",id);return access(id,subject,false);}
  return rows.getFirst();
 }
 public boolean connected(String a,String b){return db.queryForObject("select count(*) from connection_request where state='ACCEPTED' and ((sender=? and recipient=?) or (sender=? and recipient=?))",Integer.class,a,b,b,a)>0;}
 public static Message message(java.sql.ResultSet r,String subject,Access access)throws java.sql.SQLException{
  String sender=r.getString("sender");long seq=r.getLong("sequence");
  return new Message(r.getObject("id",UUID.class),seq,r.getString("body"),sender.equals(subject),seq<=access.recipientRead(sender),r.getTimestamp("created_at").toInstant().toString());
 }
}
