package com.meridian.content.adapter;
import com.meridian.content.application.PostStore;
import com.meridian.content.domain.Content.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcPostStore implements PostStore {
 private final JdbcTemplate db;
 public JdbcPostStore(JdbcTemplate db){this.db=db;}
 private static final String SELECT="select p.*,a.name,a.public_id,(select count(*) from post_reaction r where r.post_id=p.id) as reaction_count,exists(select 1 from post_reaction r where r.post_id=p.id and r.member=?) as liked,exists(select 1 from saved_post s where s.post_id=p.id and s.member=?) as saved from content_post p join profile a on a.subject=p.author ";
 private static final String VISIBLE=JdbcContentAccess.VISIBLE;
 private RowMapper<PostRow> mapper(String subject){return (r,n)->new PostRow(new Post(r.getObject("id",UUID.class),r.getString("name"),r.getObject("public_id",UUID.class),r.getString("body"),Audience.valueOf(r.getString("audience")),r.getLong("version"),r.getTimestamp("created_at").toInstant().toString(),r.getBoolean("edited"),subject.equals(r.getString("author")),r.getLong("reaction_count"),r.getBoolean("liked"),r.getBoolean("saved")),r.getLong("sequence"));}
 public List<PostRow> list(String subject,Long before,boolean savedOnly,boolean followedOnly,boolean hiddenOnly){
  var args=new ArrayList<Object>(List.of(subject,subject,subject,subject,subject));
  String filter=savedOnly?" and exists(select 1 from saved_post s where s.post_id=p.id and s.member=?)":"";
  if(savedOnly)args.add(subject);
  if(followedOnly){filter+=" and exists(select 1 from member_follow f where f.follower=? and f.followed=p.author)";args.add(subject);}
  if(!savedOnly){filter+=" and "+(hiddenOnly?"":"not ")+"exists(select 1 from hidden_post h where h.post_id=p.id and h.member=?)";args.add(subject);}
  args.add(before==null?Long.MAX_VALUE:before);
  var rows=db.query(SELECT+"where "+VISIBLE+filter+" and p.sequence < ? order by p.sequence desc limit 21",mapper(subject),args.toArray());
  return rows;
 }
 public List<PostRow> search(String subject,String query,Long before){return db.query(SELECT+"where "+VISIBLE+" and strpos(lower(p.body),lower(?))>0 and p.sequence<? order by p.sequence desc limit 21",mapper(subject),subject,subject,subject,subject,subject,query,before==null?Long.MAX_VALUE:before);}
 public List<PostRow> activity(String subject,UUID id,Long before){return db.query(SELECT+"where a.public_id=? and "+VISIBLE+" and p.sequence<? order by p.sequence desc limit 21",mapper(subject),subject,subject,id,subject,subject,subject,before==null?Long.MAX_VALUE:before);}
 public Optional<Post> read(String subject,UUID id){
  var rows=db.query(SELECT+"where p.id=? and "+VISIBLE,mapper(subject),subject,subject,id,subject,subject,subject);
  return rows.stream().map(PostRow::post).findFirst();
 }
 public void insert(String subject,UUID id,String text,Audience audience){db.update("insert into content_post(id,author,body,audience) select ?,subject,?,? from profile where subject=? on conflict(id) do nothing",id,text,audience.name(),subject);}
 public boolean matching(String subject,UUID id,String text,Audience audience){return db.queryForObject("select count(*) from content_post where id=? and author=? and body=? and audience=? and version=1 and deleted_at is null",Integer.class,id,subject,text,audience.name())==1;}
 public boolean update(String subject,UUID id,String text,Audience audience,long version){return db.update("update content_post set body=?,audience=?,version=version+1,edited=true where id=? and author=? and version=? and deleted_at is null",text,audience.name(),id,subject,version)==1;}
 public boolean delete(String subject,UUID id,long version){return db.update("update content_post set deleted_at=now(),version=version+1 where id=? and author=? and version=? and deleted_at is null",id,subject,version)==1;}
 public boolean profileExists(String subject){return db.queryForObject("select count(*) from profile where subject=?",Integer.class,subject)>0;}
 public void marker(String subject,UUID id,Marker marker,boolean enabled){
  switch(marker){
   case HIDDEN->{if(enabled)db.update("insert into hidden_post(member,post_id) values(?,?) on conflict do nothing",subject,id);else db.update("delete from hidden_post where member=? and post_id=?",subject,id);}
   case LIKED->{if(enabled)db.update("insert into post_reaction(post_id,member) values(?,?) on conflict do nothing",id,subject);else db.update("delete from post_reaction where post_id=? and member=?",id,subject);}
   case SAVED->{if(enabled)db.update("insert into saved_post(post_id,member) values(?,?) on conflict do nothing",id,subject);else db.update("delete from saved_post where post_id=? and member=?",id,subject);}
  }
 }
}
