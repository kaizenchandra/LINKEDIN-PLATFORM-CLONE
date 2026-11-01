package com.meridian.profilesections.adapter;
import com.meridian.profilesections.application.ProfileSectionsStore;
import com.meridian.profilesections.domain.ProfileSections.*;
import java.util.*;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcProfileSectionsStore implements ProfileSectionsStore {
 private final JdbcTemplate db;
 public JdbcProfileSectionsStore(JdbcTemplate db){this.db=db;}
 public List<Experience> experience(String subject){
  return db.query("select * from experience where subject=? order by start_date desc,id",(r,n)->new Experience(r.getObject("id",UUID.class),r.getString("title"),r.getString("organization"),r.getObject("start_date",LocalDate.class),r.getObject("end_date",LocalDate.class),r.getString("description"),r.getLong("version")),subject);
 }
 public boolean save(String subject,Experience e){
  int count;
  if(e.version()==0){
   count=db.update("insert into experience(id,subject,title,organization,start_date,end_date,description,version) select ?,?,?,?,?,?,?,1 where exists(select 1 from profile where subject=?) on conflict do nothing",e.id(),subject,e.title(),e.organization(),e.startDate(),e.endDate(),e.description(),subject);
  }else{
   count=db.update("update experience set title=?,organization=?,start_date=?,end_date=?,description=?,version=version+1 where id=? and subject=? and version=?",e.title(),e.organization(),e.startDate(),e.endDate(),e.description(),e.id(),subject,e.version());
  }
  return count==1;
 }
 public List<Education> education(String subject){
  return db.query("select * from education where subject=? order by start_date desc,id",(r,n)->new Education(r.getObject("id",UUID.class),r.getString("degree"),r.getString("institution"),r.getObject("start_date",LocalDate.class),r.getObject("end_date",LocalDate.class),r.getString("description"),r.getString("field_of_study"),r.getLong("version")),subject);
 }
 public boolean save(String subject,Education e){
  int count;
  if(e.version()==0){
   count=db.update("insert into education(id,subject,degree,institution,start_date,end_date,description,field_of_study,version) select ?,?,?,?,?,?,?,?,1 where exists(select 1 from profile where subject=?) on conflict do nothing",e.id(),subject,e.degree(),e.institution(),e.startDate(),e.endDate(),e.description(),e.fieldOfStudy(),subject);
  }else{
   count=db.update("update education set degree=?,institution=?,start_date=?,end_date=?,description=?,field_of_study=?,version=version+1 where id=? and subject=? and version=?",e.degree(),e.institution(),e.startDate(),e.endDate(),e.description(),e.fieldOfStudy(),e.id(),subject,e.version());
  }
  return count==1;
 }
 public List<Certification> certifications(String subject){
  return db.query("select * from certifications where subject=? order by issue_date desc,id",(r,n)->new Certification(r.getObject("id",UUID.class),r.getString("name"),r.getString("issuer"),r.getObject("issue_date",LocalDate.class),r.getObject("expiry_date",LocalDate.class),r.getString("credential_url"),r.getString("credential_id"),r.getLong("version")),subject);
 }
 public boolean save(String subject,Certification e){
  int count;
  if(e.version()==0){
   count=db.update("insert into certifications(id,subject,name,issuer,issue_date,expiry_date,credential_url,credential_id,version) select ?,?,?,?,?,?,?,?,1 where exists(select 1 from profile where subject=?) on conflict do nothing",e.id(),subject,e.name(),e.issuer(),e.issueDate(),e.expiryDate(),e.credentialUrl(),e.credentialId(),subject);
  }else{
   count=db.update("update certifications set name=?,issuer=?,issue_date=?,expiry_date=?,credential_url=?,credential_id=?,version=version+1 where id=? and subject=? and version=?",e.name(),e.issuer(),e.issueDate(),e.expiryDate(),e.credentialUrl(),e.credentialId(),e.id(),subject,e.version());
  }
  return count==1;
 }
 public List<Project> projects(String subject){
  return db.query("select * from projects where subject=? order by start_date desc,id",(r,n)->new Project(r.getObject("id",UUID.class),r.getString("title"),r.getString("organization"),r.getObject("start_date",LocalDate.class),r.getObject("end_date",LocalDate.class),r.getString("description"),r.getString("associated_skills"),r.getLong("version")),subject);
 }
 public boolean save(String subject,Project e){
  int count;
  if(e.version()==0){
   count=db.update("insert into projects(id,subject,title,organization,start_date,end_date,description,associated_skills,version) select ?,?,?,?,?,?,?,?,1 where exists(select 1 from profile where subject=?) on conflict do nothing",e.id(),subject,e.title(),e.organization(),e.startDate(),e.endDate(),e.description(),e.associatedSkills(),subject);
  }else{
   count=db.update("update projects set title=?,organization=?,start_date=?,end_date=?,description=?,associated_skills=?,version=version+1 where id=? and subject=? and version=?",e.title(),e.organization(),e.startDate(),e.endDate(),e.description(),e.associatedSkills(),e.id(),subject,e.version());
  }
  return count==1;
 }
 public List<Skill> skills(String subject){
  return db.query("select id,name,version from profile_skill where subject=? order by name_key,id",(r,n)->new Skill(r.getObject("id",UUID.class),r.getString("name"),r.getLong("version")),subject);
 }
 public boolean save(String subject,Skill skill){
  String name=skill.name(),key=name.toLowerCase(Locale.ROOT);
  try{
   int changed=skill.version()==0
    ? db.update("insert into profile_skill(id,subject,name,name_key,version) select ?,?,?,?,1 where exists(select 1 from profile where subject=?) on conflict do nothing",skill.id(),subject,name,key,subject)
    : db.update("update profile_skill set name=?,name_key=?,version=version+1 where id=? and subject=? and version=?",name,key,skill.id(),subject,skill.version());
   return changed==1;
  }catch(DuplicateKeyException duplicate){return false;}
 }
 public List<Language> languages(String subject){
  return db.query("select id,name,proficiency,version from profile_language where subject=? order by name_key,id",(r,n)->new Language(r.getObject("id",UUID.class),r.getString("name"),Proficiency.valueOf(r.getString("proficiency")),r.getLong("version")),subject);
 }
 public boolean save(String subject,Language language){
  String name=language.name(),key=name.toLowerCase(Locale.ROOT);
  try{
   int changed=language.version()==0
    ? db.update("insert into profile_language(id,subject,name,name_key,proficiency,version) select ?,?,?,?,?,1 where exists(select 1 from profile where subject=?) on conflict do nothing",language.id(),subject,name,key,language.proficiency().name(),subject)
    : db.update("update profile_language set name=?,name_key=?,proficiency=?,version=version+1 where id=? and subject=? and version=?",name,key,language.proficiency().name(),language.id(),subject,language.version());
   return changed==1;
  }catch(DuplicateKeyException duplicate){return false;}
 }
 public boolean delete(String subject,Section section,UUID id,long version){return switch(section){
  case EXPERIENCE->db.update("delete from experience where id=? and subject=? and version=?",id,subject,version)==1;
  case EDUCATION->db.update("delete from education where id=? and subject=? and version=?",id,subject,version)==1;
  case CERTIFICATION->db.update("delete from certifications where id=? and subject=? and version=?",id,subject,version)==1;
  case PROJECT->db.update("delete from projects where id=? and subject=? and version=?",id,subject,version)==1;
  case SKILL->db.update("delete from profile_skill where id=? and subject=? and version=?",id,subject,version)==1;
  case LANGUAGE->db.update("delete from profile_language where id=? and subject=? and version=?",id,subject,version)==1;
 };}
}
