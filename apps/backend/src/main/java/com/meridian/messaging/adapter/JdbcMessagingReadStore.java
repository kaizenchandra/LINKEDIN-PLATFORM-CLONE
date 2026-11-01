package com.meridian.messaging.adapter;
import java.util.*;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.messaging.application.MessagingReadStore;
import static com.meridian.messaging.adapter.JdbcMessagingAccess.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcMessagingReadStore implements MessagingReadStore {
 private final JdbcTemplate db;private final JdbcMessagingAccess access;
 public JdbcMessagingReadStore(JdbcTemplate db,JdbcMessagingAccess access){this.db=db;this.access=access;}
 public List<ConversationRow> list(String subject,Long before,View view,String query){
  String field=view==View.STARRED?"starred":"archived";
  String filter=" and "+(view==View.INBOX?"not ":"")+"(case when c.member_a=? then c."+field+"_a else c."+field+"_b end)";
  var rows=db.query("select c.*,p.public_id,p.name,"+CONNECTED+" as can_send,(select count(*) from direct_message m where m.conversation_id=c.id and m.sender<>? and m.sequence>case when c.member_a=? then c.read_a else c.read_b end) as unread from direct_conversation c join profile p on p.subject=case when c.member_a=? then c.member_b else c.member_a end where ? in(c.member_a,c.member_b) and "+UNBLOCKED+filter+" and strpos(lower(p.name),lower(?))>0 and c.sequence<? order by c.sequence desc limit 21",(r,i)->new ConversationRow(new Conversation(r.getObject("id",UUID.class),r.getObject("public_id",UUID.class),r.getString("name"),r.getLong("unread"),r.getBoolean("can_send"),new Preference(r.getBoolean("starred_"+(subject.equals(r.getString("member_a"))?"a":"b")),r.getBoolean("archived_"+(subject.equals(r.getString("member_a"))?"a":"b")),r.getLong("preference_version_"+(subject.equals(r.getString("member_a"))?"a":"b")))),r.getLong("sequence")),subject,subject,subject,subject,subject,query,before==null?Long.MAX_VALUE:before);
  return rows;
 }
 public Access access(UUID id,String subject){return access.access(id,subject,false);}
 public boolean connected(String a,String b){return access.connected(a,b);}
 public List<Message> messages(UUID id,String subject,Long before,Access allowed){return db.query("select * from direct_message where conversation_id=? and sequence<? and sequence<=? order by sequence desc limit 51",(r,i)->message(r,subject,allowed),id,before==null?Long.MAX_VALUE:before,allowed.latest());}
 public Person person(String subject){return db.queryForObject("select public_id,name from profile where subject=?",(r,i)->new Person(r.getObject("public_id",UUID.class),r.getString("name")),subject);}
}
