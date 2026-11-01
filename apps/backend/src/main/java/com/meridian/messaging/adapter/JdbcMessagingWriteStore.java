package com.meridian.messaging.adapter;
import java.util.*;
import com.meridian.messaging.application.MessagingWriteStore;
import com.meridian.messaging.domain.PreviousMessage;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.shared.adapter.MemberPairLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcMessagingWriteStore implements MessagingWriteStore {
 private final JdbcTemplate db;private final JdbcMessagingAccess access;
 public JdbcMessagingWriteStore(JdbcTemplate db,JdbcMessagingAccess access){this.db=db;this.access=access;}
 public Optional<String> target(UUID member,String subject){return db.queryForList("select subject from profile where public_id=? and subject<>?",String.class,member,subject).stream().findFirst();}
 public void lockPair(String a,String b){MemberPairLock.acquire(db,a,b);}
 public boolean blocked(String a,String b){return db.queryForObject("select count(*) from member_block where (blocker=? and blocked=?) or (blocker=? and blocked=?)",Integer.class,a,b,b,a)>0;}
 public boolean connected(String a,String b){return access.connected(a,b);}
 public UUID start(String a,String b){db.update("insert into direct_conversation(id,member_a,member_b) values(?,least(?,?),greatest(?,?)) on conflict(member_a,member_b) do nothing",UUID.randomUUID(),a,b,a,b);return db.queryForObject("select id from direct_conversation where member_a=least(?,?) and member_b=greatest(?,?)",UUID.class,a,b,a,b);}
 public Access access(UUID id,String subject,boolean lock){return access.access(id,subject,lock);}
 public Optional<PreviousMessage> previous(UUID id){return db.query("select conversation_id,sender,body from direct_message where id=?",(r,i)->new PreviousMessage(r.getObject("conversation_id",UUID.class),r.getString("sender"),r.getString("body")),id).stream().findFirst();}
 public boolean insert(UUID message,UUID conversation,long sequence,String subject,String body){return db.update("insert into direct_message(id,conversation_id,sequence,sender,body) values(?,?,?,?,?) on conflict(id) do nothing",message,conversation,sequence,subject,body)==1;}
 public void advance(UUID conversation){db.update("update direct_conversation set last_sequence=last_sequence+1,preference_version_a=preference_version_a+case when archived_a then 1 else 0 end,preference_version_b=preference_version_b+case when archived_b then 1 else 0 end,archived_a=false,archived_b=false where id=?",conversation);}
 public Message receipt(UUID message,String subject,Access allowed){return db.queryForObject("select * from direct_message where id=?",(r,i)->JdbcMessagingAccess.message(r,subject,allowed),message);}
 public boolean preferences(UUID id,boolean first,Preference desired){String suffix=first?"a":"b";return db.update("update direct_conversation set starred_"+suffix+"=?,archived_"+suffix+"=?,preference_version_"+suffix+"=preference_version_"+suffix+"+1 where id=? and preference_version_"+suffix+"=?",desired.starred(),desired.archived(),id,desired.version())==1;}
 public void read(UUID id,boolean first,long through){String column=first?"read_a":"read_b";db.update("update direct_conversation set "+column+"=greatest("+column+",?) where id=?",through,id);}
}
