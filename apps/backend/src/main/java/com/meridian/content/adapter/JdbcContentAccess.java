package com.meridian.content.adapter;
import com.meridian.content.application.ContentAccess;
import com.meridian.shared.adapter.MemberPairLock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcContentAccess implements ContentAccess {
 private final JdbcTemplate db;
 public JdbcContentAccess(JdbcTemplate db){this.db=db;}
 public static final String VISIBLE="p.deleted_at is null and p.moderated_at is null and (p.author=? or p.audience='MEMBERS') and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=p.author) or (b.blocked=? and b.blocker=p.author))";
 public boolean visible(String subject,UUID id){return db.queryForObject("select count(*) from content_post p where p.id=? and "+VISIBLE,Integer.class,id,subject,subject,subject)>0;}
 public boolean lockVisible(String subject,UUID id){
  var authors=db.queryForList("select author from content_post where id=?",String.class,id);if(authors.isEmpty())return false;
  MemberPairLock.acquire(db,subject,authors.getFirst());db.queryForList("select id from content_post where id=? for update",id);return visible(subject,id);
 }
}
