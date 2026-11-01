package com.meridian.recruiting.adapter;
import java.util.*;
import com.meridian.shared.adapter.MemberPairLock;
import com.meridian.recruiting.application.RecruiterStore;
import com.meridian.recruiting.domain.RecruiterAccess.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcRecruiterStore implements RecruiterStore {
 private final JdbcTemplate db;
 public JdbcRecruiterStore(JdbcTemplate db){this.db=db;}
 private static final String SELECT="select r.*,o.name as company_name,p.name as member_name,o.owner_subject=? as owned from recruiter_access r join organization o on o.id=r.organization_id join profile p on p.subject=r.member";
 private static Access row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new Access(r.getObject("id",UUID.class),r.getObject("organization_id",UUID.class),r.getString("company_name"),r.getString("member_name"),r.getString("state"),r.getLong("version"),r.getBoolean("owned"));}

 public boolean owns(String subject,UUID organization){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from organization where id=? and owner_subject=?)",Boolean.class,organization,subject));}
 public List<Access> list(String subject,UUID organization,UUID after){
  String filter=" where r.member=?";var args=new ArrayList<Object>(List.of(subject,subject));
  if(organization!=null){filter=" where r.organization_id=?";args.set(1,organization);}
  if(after!=null){filter+=" and r.id>?";args.add(after);}
  return db.query(SELECT+filter+" order by r.id limit 21",JdbcRecruiterStore::row,args.toArray());
 }
 public Optional<String> lockOrganization(UUID organization){return db.queryForList("select owner_subject from organization where id=? for update",String.class,organization).stream().findFirst();}
 public Optional<Invitation> invitation(UUID id){return db.query("select organization_id,connection_id from recruiter_access where id=?",(r,i)->new Invitation(r.getObject(1,UUID.class),r.getObject(2,UUID.class)),id).stream().findFirst();}
 public Optional<String> connectedMember(UUID connection,String subject){return db.query("select sender,recipient from connection_request where id=? and (sender=? or recipient=?)",(r,i)->subject.equals(r.getString("sender"))?r.getString("recipient"):r.getString("sender"),connection,subject,subject).stream().findFirst();}
 public void lockPair(String subject,String member){MemberPairLock.acquire(db,subject,member);}
 public boolean acceptedUnblocked(UUID connection,String subject,String member){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from connection_request c where c.id=? and c.state='ACCEPTED' and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=?) or (b.blocker=? and b.blocked=?)))",Boolean.class,connection,subject,member,member,subject));}
 public int occupied(UUID organization){return db.queryForObject("select count(*) from recruiter_access where organization_id=? and state in ('PENDING','ACTIVE')",Integer.class,organization);}
 public boolean insert(UUID id,UUID organization,String member,UUID connection){return db.update("insert into recruiter_access(id,organization_id,member,connection_id) values(?,?,?,?) on conflict do nothing",id,organization,member,connection)==1;}
 public Access receipt(UUID id,String subject){return db.queryForObject(SELECT+" where r.id=? and (r.member=? or o.owner_subject=?)",JdbcRecruiterStore::row,subject,id,subject,subject);}
 public Optional<UUID> accessibleOrganization(UUID id,String subject){return db.queryForList("select r.organization_id from recruiter_access r join organization o on o.id=r.organization_id where r.id=? and (r.member=? or o.owner_subject=?)",UUID.class,id,subject,subject).stream().findFirst();}
 public Membership lockMembership(UUID id){return db.queryForObject("select member,state,version from recruiter_access where id=? for update",(r,i)->new Membership(r.getString(1),r.getString(2),r.getLong(3)),id);}
 public void change(UUID id,String state){db.update("update recruiter_access set state=?,version=version+1 where id=?",state,id);}
 public void audit(UUID id,long version,String subject,String before,String after){db.update("insert into recruiter_access_audit(access_id,version,actor,from_state,to_state) values(?,?,?,?,?)",id,version,subject,before,after);}
}
