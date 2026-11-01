package com.meridian.peoplesearch.adapter;
import com.meridian.peoplesearch.application.PeopleStore;
import com.meridian.peoplesearch.domain.People.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcPeopleStore implements PeopleStore {
 private final JdbcTemplate db;
 public JdbcPeopleStore(JdbcTemplate db){this.db=db;}
 public Optional<Discovery> discovery(String subject){return db.query("select discoverable,discovery_version from profile where subject=?",(r,i)->new Discovery(r.getBoolean(1),r.getLong(2)),subject).stream().findFirst();}
 public boolean discovery(String subject,boolean enabled,long version){return db.update("update profile set discoverable=?,discovery_version=discovery_version+1 where subject=? and discovery_version=?",enabled,subject,version)==1;}
 public List<Person> search(String subject,Criteria criteria,UUID after){
  var args=new ArrayList<Object>(List.of(subject,criteria.query(),criteria.location(),subject,subject));
  String cursor="";if(after!=null){cursor=" and p.public_id>?";args.add(after);}
  var rows=db.query("select p.public_id,p.name,p.headline,p.location from profile p where p.member_visible and p.discoverable and p.subject<>? and strpos(lower(p.name||' '||p.headline),lower(?))>0 and strpos(lower(p.location),lower(?))>0 and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=p.subject) or (b.blocked=? and b.blocker=p.subject))"+cursor+" order by p.public_id limit 21",(r,i)->new Person(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4)),args.toArray());
 return rows;
 }
}
