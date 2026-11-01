package com.meridian.profile.adapter;
import com.meridian.profile.application.ProfileStore;
import com.meridian.profile.domain.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcProfileStore implements ProfileStore {
 private final JdbcTemplate db;
 public JdbcProfileStore(JdbcTemplate db){this.db=db;}
 public void initialize(String subject,String name){db.update("insert into profile(subject,name) values (?,?) on conflict do nothing",subject,name);}
 public Profile get(String subject){return db.queryForObject("select * from profile where subject=?",(r,n)->new Profile(r.getString("name"),r.getString("headline"),r.getString("location"),r.getString("about"),r.getLong("version")),subject);}
 public boolean update(String subject,Profile p){return db.update("update profile set name=?,headline=?,location=?,about=?,version=version+1 where subject=? and version=?",p.name(),p.headline(),p.location(),p.about(),subject,p.version())==1;}
}
