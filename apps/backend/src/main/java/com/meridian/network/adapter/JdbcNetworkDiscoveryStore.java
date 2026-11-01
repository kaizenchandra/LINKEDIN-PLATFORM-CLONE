package com.meridian.network.adapter;
import com.meridian.network.application.NetworkDiscoveryStore;
import com.meridian.network.domain.Network.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcNetworkDiscoveryStore implements NetworkDiscoveryStore {
 private final JdbcTemplate db;
 public JdbcNetworkDiscoveryStore(JdbcTemplate db){this.db=db;}
 private static String unblocked(String left,String right){return "not exists(select 1 from member_block b where (b.blocker="+left+" and b.blocked="+right+") or (b.blocked="+left+" and b.blocker="+right+"))";}
 private static String connected(String left,String right){return "exists(select 1 from connection_request c where c.state='ACCEPTED' and ((c.sender="+left+" and c.recipient="+right+") or (c.recipient="+left+" and c.sender="+right+")))";}
 // Only mutually consenting, discoverable profiles can disclose their connection edges.
 private static final String MUTUAL="select count(*) from profile m where m.member_visible and m.discoverable and m.connections_visible and m.subject<>viewer.subject and m.subject<>p.subject and "+connected("m.subject","viewer.subject")+" and "+connected("m.subject","p.subject")+" and "+unblocked("m.subject","viewer.subject")+" and "+unblocked("m.subject","p.subject");

 public Optional<Sharing> privacy(String subject){return db.query("select connections_visible,connections_version from profile where subject=?",(r,i)->new Sharing(r.getBoolean(1),r.getLong(2)),subject).stream().findFirst();}
 public boolean privacy(String subject,boolean visible,long version){return db.update("update profile set connections_visible=?,connections_version=connections_version+1 where subject=? and connections_version=?",visible,subject,version)==1;}
 public List<SuggestedMember> suggestions(String subject,UUID after){
  var args=new ArrayList<Object>(List.of(subject));String cursor="";if(after!=null){cursor=" and p.public_id>?";args.add(after);}
  var rows=db.query("select p.public_id,p.name,p.headline,p.location,case when p.connections_visible then ("+MUTUAL+") else 0 end as mutual_count from profile p cross join profile viewer where viewer.subject=? and p.subject<>viewer.subject and p.member_visible and p.discoverable and "+unblocked("p.subject","viewer.subject")+" and not exists(select 1 from connection_request c where c.state in ('PENDING','ACCEPTED') and ((c.sender=viewer.subject and c.recipient=p.subject) or (c.recipient=viewer.subject and c.sender=p.subject)))"+cursor+" order by p.public_id limit 21",(r,i)->new SuggestedMember(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),r.getLong(5)),args.toArray());
  return rows;
 }
 public Optional<List<SuggestedMember>> mutual(String subject,UUID id,UUID after){
  var args=new ArrayList<Object>(List.of(subject,id));String cursor="";if(after!=null){cursor=" and m.public_id>?";args.add(after);}
  String target="select p.subject from profile p cross join profile viewer where viewer.subject=? and p.public_id=? and p.subject<>viewer.subject and p.member_visible and p.discoverable and p.connections_visible and "+unblocked("p.subject","viewer.subject");
  String query="with target as ("+target+") select target.subject as allowed,m.public_id,m.name,m.headline,m.location from target left join lateral (select m.* from profile m cross join profile viewer where viewer.subject=? and m.subject<>viewer.subject and m.subject<>target.subject and m.member_visible and m.discoverable and m.connections_visible and "+connected("m.subject","viewer.subject")+" and "+connected("m.subject","target.subject")+" and "+unblocked("m.subject","viewer.subject")+" and "+unblocked("m.subject","target.subject")+cursor+" order by m.public_id limit 21) m on true order by m.public_id";
  args.add(2,subject);
  var rows=db.queryForList(query,args.toArray());if(rows.isEmpty())return Optional.empty();
  return Optional.of(rows.stream().filter(r->r.get("public_id")!=null).map(r->new SuggestedMember((UUID)r.get("public_id"),(String)r.get("name"),(String)r.get("headline"),(String)r.get("location"),0)).toList());
 }
 public List<FollowRow> followers(String subject,Long before){
  String filter=" f.followed=? and "+unblocked("f.follower","f.followed");
  var rows=db.query("select p.public_id,case when p.member_visible then p.name else 'Private member' end,p.member_visible,f.sequence from member_follow f join profile p on p.subject=f.follower where "+filter+" and f.sequence<? order by f.sequence desc limit 21",(r,i)->new FollowRow(new Member(r.getObject(1,UUID.class),r.getString(2),r.getBoolean(3)),r.getLong(4)),subject,before==null?Long.MAX_VALUE:before);
  return rows;
 }
 public long followerCount(String subject){String filter=" f.followed=? and "+unblocked("f.follower","f.followed");return db.queryForObject("select count(*) from member_follow f where "+filter,Long.class,subject);}
}
