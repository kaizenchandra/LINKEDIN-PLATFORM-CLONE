package com.meridian.community.adapter;
import com.meridian.community.application.CommunityStore;
import com.meridian.community.domain.Community.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcCommunityStore implements CommunityStore {
 private final JdbcTemplate db;
 public JdbcCommunityStore(JdbcTemplate db){this.db=db;}
 public List<Update> catchUp(String subject){return db.query("select p.id,p.body,p.created_at,m.public_id,m.name from content_post p join profile m on m.subject=p.author where p.deleted_at is null and p.moderated_at is null and p.audience='MEMBERS' and p.created_at>now()-interval '30 days' and exists(select 1 from connection_request c where c.state='ACCEPTED' and ((c.sender=? and c.recipient=p.author) or (c.recipient=? and c.sender=p.author))) and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=p.author) or (b.blocked=? and b.blocker=p.author)) order by p.created_at desc,p.id limit 50",(r,n)->new Update(r.getObject("id",UUID.class),r.getObject("public_id",UUID.class),r.getString("name"),r.getString("body"),r.getTimestamp("created_at").toInstant()),subject,subject,subject,subject);}
 public List<Entry> list(String subject,Kind kind){return db.query("select c.*,exists(select 1 from community_membership m where m.entry_id=c.id and m.subject=?) joined,(select count(*) from community_membership m where m.entry_id=c.id) members from community_entry c where kind=? order by created_at desc,id limit 100",(r,n)->new Entry(r.getObject("id",UUID.class),Kind.valueOf(r.getString("kind")),r.getString("title"),r.getString("description"),r.getTimestamp("starts_at")==null?null:r.getTimestamp("starts_at").toInstant(),r.getString("location"),r.getBoolean("joined"),r.getLong("members"),subject.equals(r.getString("owner"))),subject,kind.name());}
 public boolean profileExists(String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from profile where subject=?)",Boolean.class,subject));}
 public boolean create(String subject,Draft d){return db.update("insert into community_entry(id,owner,kind,title,description,starts_at,location) values(?,?,?,?,?,?,?) on conflict do nothing",d.id(),subject,d.kind().name(),d.title().strip(),d.description().strip(),d.startsAt()==null?null:java.sql.Timestamp.from(d.startsAt()),d.location().strip())==1;}
 public boolean exists(UUID id){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from community_entry where id=?)",Boolean.class,id));}
 public void membership(String subject,UUID id,boolean joined){if(joined)db.update("insert into community_membership(entry_id,subject) values(?,?) on conflict do nothing",id,subject);else db.update("delete from community_membership where entry_id=? and subject=?",id,subject);}
}
