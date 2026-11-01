package com.meridian.organizations.adapter;
import com.meridian.organizations.application.OrganizationStore;
import com.meridian.organizations.domain.Organizations.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcOrganizationStore implements OrganizationStore {
 private final JdbcTemplate db;
 public JdbcOrganizationStore(JdbcTemplate db){this.db=db;}
 private static final String SELECT="select o.*,o.owner_subject=? as owned,exists(select 1 from organization_follow f where f.organization_id=o.id and f.subject=?) as following,(select count(*) from organization_follow f where f.organization_id=o.id) as follower_count,"+OrganizationRecruitingSql.SQL+" as can_recruit from organization o";
 private static Organization row(java.sql.ResultSet r,int index)throws java.sql.SQLException{return new Organization(r.getObject("id",UUID.class),r.getString("name"),r.getString("industry"),r.getString("location"),r.getString("about"),r.getLong("version"),r.getBoolean("owned"),r.getBoolean("following"),r.getLong("follower_count"),r.getBoolean("can_recruit"));}

 public boolean initialized(String subject){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from profile where subject=?)",Boolean.class,subject));}
 public List<Organization> list(String subject,String query,View view,UUID after){
  var args=new ArrayList<Object>(List.of(subject,subject,subject,query,query));
  String filter=" where (strpos(lower(o.name),lower(?))>0 or strpos(lower(o.industry),lower(?))>0)";
  if(view==View.MANAGED){filter+=" and o.owner_subject=?";args.add(subject);}
  if(view==View.FOLLOWING){filter+=" and exists(select 1 from organization_follow f where f.organization_id=o.id and f.subject=?)";args.add(subject);}
  if(after!=null){filter+=" and o.id>?";args.add(after);}
  var rows=db.query(SELECT+filter+" order by o.id limit 21",JdbcOrganizationStore::row,args.toArray());
 return rows;
 }
 public Optional<Organization> get(String subject,UUID id){return db.query(SELECT+" where o.id=?",JdbcOrganizationStore::row,subject,subject,subject,id).stream().findFirst();}
 public boolean insert(String subject,UUID id,Write value){return db.update("insert into organization(id,owner_subject,name,industry,location,about) values(?,?,?,?,?,?) on conflict(id) do nothing",id,subject,value.name(),value.industry(),value.location(),value.about())==1;}
 public Optional<String> lockOwner(UUID id){return db.queryForList("select owner_subject from organization where id=? for update",String.class,id).stream().findFirst();}
 public boolean update(UUID id,Write value){return db.update("update organization set name=?,industry=?,location=?,about=?,version=version+1,updated_at=now() where id=? and version=?",value.name(),value.industry(),value.location(),value.about(),id,value.version())==1;}
 public void follow(String subject,UUID id){db.update("insert into organization_follow(organization_id,subject) values(?,?) on conflict do nothing",id,subject);}
 public void unfollow(String subject,UUID id){db.update("delete from organization_follow where organization_id=? and subject=?",id,subject);}
}
