package com.meridian.content.adapter;
import com.meridian.content.application.CommentStore;
import com.meridian.content.domain.Content.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcCommentStore implements CommentStore {
 private final JdbcTemplate db;
 public JdbcCommentStore(JdbcTemplate db){this.db=db;}
 private RowMapper<CommentRow> mapper(String subject){return (r,n)->new CommentRow(new Comment(r.getObject("id",UUID.class),r.getObject("parent_id",UUID.class),r.getString("name"),r.getString("body"),r.getTimestamp("created_at").toInstant().toString(),subject.equals(r.getString("author"))),r.getLong("sequence"));}
 public List<CommentRow> list(String subject,UUID postId,Long before,UUID parentId){return db.query("select c.*,a.name from post_comment c join profile a on a.subject=c.author join content_post p on p.id=c.post_id where p.id=? and c.parent_id is not distinct from ? and (c.parent_id is null or exists(select 1 from post_comment parent where parent.id=c.parent_id and parent.deleted_at is null and parent.parent_id is null and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=parent.author) or (b.blocked=? and b.blocker=parent.author)))) and c.deleted_at is null and "+JdbcContentAccess.VISIBLE+" and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=c.author) or (b.blocked=? and b.blocker=c.author)) and c.sequence < ? order by c.sequence desc limit 21",mapper(subject),postId,parentId,subject,subject,subject,subject,subject,subject,subject,before==null?Long.MAX_VALUE:before);}
 public boolean parentVisible(String subject,UUID postId,UUID parentId){return db.queryForObject("select count(*) from post_comment c where c.id=? and c.post_id=? and c.parent_id is null and c.deleted_at is null and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=c.author) or (b.blocked=? and b.blocker=c.author))",Integer.class,parentId,postId,subject,subject)==1;}
 public void insert(String subject,UUID postId,UUID id,String text,UUID parentId){db.update("insert into post_comment(id,post_id,author,body,parent_id) select ?,?,subject,?,? from profile where subject=? on conflict(id) do nothing",id,postId,text,parentId,subject);}
 public Optional<Comment> matching(String subject,UUID postId,UUID id,String text,UUID parentId){return db.query("select c.*,a.name from post_comment c join profile a on a.subject=c.author where c.id=? and c.post_id=? and c.author=? and c.body=? and c.parent_id is not distinct from ? and c.deleted_at is null",mapper(subject),id,postId,subject,text,parentId).stream().map(CommentRow::comment).findFirst();}
 public boolean delete(String subject,UUID postId,UUID id){return db.update("update post_comment set deleted_at=coalesce(deleted_at,now()) where id=? and post_id=? and author=?",id,postId,subject)==1;}
}
