package com.meridian.moderation.adapter;
import com.meridian.moderation.application.ModerationStore;
import com.meridian.moderation.domain.Moderation.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcModerationStore implements ModerationStore {
 private final JdbcTemplate db;
 public JdbcModerationStore(JdbcTemplate db){this.db=db;}
 private static Report row(java.sql.ResultSet r)throws java.sql.SQLException{return new Report(r.getObject("id",UUID.class),r.getObject("post_id",UUID.class),r.getString("category"),r.getString("state"),r.getLong("version"),r.getTimestamp("created_at").toInstant().toString());}
 public boolean operator(String subject,boolean lock){return !db.queryForList("select subject from moderation_operator where subject=?"+(lock?" for share":""),String.class,subject).isEmpty();}
 public List<Report> reports(String subject,UUID after){var args=new ArrayList<Object>(List.of(subject));if(after!=null)args.add(after);return db.query("select * from content_report where reporter=?"+(after==null?"":" and id>?")+" order by id limit 21",(r,n)->row(r),args.toArray());}
 public Optional<Report> matching(UUID id,String subject,UUID postId,Category category,String details){return db.query("select * from content_report where id=? and reporter=? and post_id=? and category=? and details=?",(r,n)->row(r),id,subject,postId,category.name(),details).stream().findFirst();}
 public boolean authored(String subject,UUID postId){return db.queryForObject("select count(*) from content_post where id=? and author=?",Integer.class,postId,subject)>0;}
 public boolean lockProfile(String subject){return !db.queryForList("select subject from profile where subject=? for update",String.class,subject).isEmpty();}
 public int dailyCount(String subject){return db.queryForObject("select count(*) from content_report where reporter=? and created_at>now()-interval '1 day'",Integer.class,subject);}
 public boolean insert(UUID id,String subject,UUID postId,Category category,String details){return db.update("insert into content_report(id,reporter,post_id,category,details) values(?,?,?,?,?) on conflict do nothing",id,subject,postId,category.name(),details)==1;}
 public Report report(UUID id){return db.queryForObject("select * from content_report where id=?",(r,n)->row(r),id);}
 public List<Item> queue(UUID after){var args=new ArrayList<Object>();if(after!=null)args.add(after);return db.query("select r.*,p.body,p.version as post_version,a.name,p.deleted_at is not null or p.moderated_at is not null as unavailable from content_report r join content_post p on p.id=r.post_id join profile a on a.subject=p.author where r.state='OPEN'"+(after==null?"":" and r.id>?")+" order by r.id limit 21",(r,n)->new Item(row(r),r.getString("details"),r.getString("body"),r.getLong("post_version"),r.getString("name"),r.getBoolean("unavailable")),args.toArray());}
 public Optional<ReviewTarget> lockReport(UUID id){
  var targets=db.queryForList("select post_id from content_report where id=?",UUID.class,id);if(targets.isEmpty())return Optional.empty();
  db.queryForList("select id from content_post where id=? for update",targets.getFirst());
  return db.query("select r.*,p.author,p.version as post_version from content_report r join content_post p on p.id=r.post_id where r.id=? for update of r",(r,n)->new ReviewTarget(r.getObject("post_id",UUID.class),r.getString("author"),r.getString("reporter"),r.getString("state"),r.getLong("version"),r.getLong("post_version")),id).stream().findFirst();
 }
 public boolean auditMatches(UUID id,String subject,Decision decision,String reason,long postVersion){return db.queryForObject("select count(*) from moderation_audit where report_id=? and reviewer=? and decision=? and reason=? and post_version=?",Integer.class,id,subject,decision.name(),reason,postVersion)==1;}
 public void removePost(UUID postId){db.update("update content_post set moderated_at=coalesce(moderated_at,now()) where id=?",postId);}
 public void decide(UUID id,Decision decision){db.update("update content_report set state=?,version=version+1 where id=?",decision.name(),id);}
 public void audit(UUID id,String subject,Decision decision,String reason,long postVersion){db.update("insert into moderation_audit(report_id,reviewer,decision,reason,post_version) values(?,?,?,?,?)",id,subject,decision.name(),reason,postVersion);}
}
