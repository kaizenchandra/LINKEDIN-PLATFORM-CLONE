package com.meridian.content.adapter;
import com.meridian.content.application.DraftStore;
import com.meridian.content.domain.Content.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcDraftStore implements DraftStore {
 private final JdbcTemplate db;
 public JdbcDraftStore(JdbcTemplate db){this.db=db;}
 private static final RowMapper<Draft> MAPPER=(r,n)->new Draft(r.getObject("id",UUID.class),r.getString("body"),Audience.valueOf(r.getString("audience")),r.getLong("version"),r.getBoolean("published"),r.getTimestamp("updated_at").toInstant().toString());
 public Optional<Draft> owned(String subject,UUID id,boolean lock){return db.query("select * from post_draft where author=? and id=?"+(lock?" for update":""),MAPPER,subject,id).stream().findFirst();}
 public List<Draft> list(String subject,UUID after){var args=new ArrayList<Object>(List.of(subject));if(after!=null)args.add(after);return db.query("select * from post_draft where author=? and not published"+(after==null?"":" and id>?")+" order by id limit 21",MAPPER,args.toArray());}
 public boolean lockProfile(String subject){return !db.queryForList("select subject from profile where subject=? for update",String.class,subject).isEmpty();}
 public boolean exists(UUID id){return !db.query("select * from post_draft where id=?",MAPPER,id).isEmpty();}
 public int count(String subject){return db.queryForObject("select count(*) from post_draft where author=? and not published",Integer.class,subject);}
 public boolean insert(String subject,UUID id,String text,Audience audience){return db.update("insert into post_draft(id,author,body,audience) values(?,?,?,?) on conflict do nothing",id,subject,text,audience.name())==1;}
 public boolean update(String subject,UUID id,String text,Audience audience,long version){return db.update("update post_draft set body=?,audience=?,version=version+1,updated_at=now() where id=? and author=? and version=? and not published",text,audience.name(),id,subject,version)==1;}
 public boolean delete(String subject,UUID id,long version){return db.update("delete from post_draft where id=? and author=? and version=? and not published",id,subject,version)==1;}
 public boolean createPost(String subject,Draft draft){return db.update("insert into content_post(id,author,body,audience) values(?,?,?,?) on conflict do nothing",draft.id(),subject,draft.text().strip(),draft.audience().name())==1;}
 public void markPublished(UUID id){db.update("update post_draft set published=true,version=version+1,updated_at=now() where id=?",id);}
}
