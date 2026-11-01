package com.meridian.account.adapter;
import com.meridian.account.application.AccountSettingsStore;
import com.meridian.account.domain.AccountSettings.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcAccountSettingsStore implements AccountSettingsStore {
 private final JdbcTemplate db;
 public JdbcAccountSettingsStore(JdbcTemplate db){this.db=db;}
 public Optional<Preferences> initializePreferences(String subject){
  db.update("insert into account_preferences(subject) select subject from profile where subject=? on conflict do nothing",subject);
  return db.query("select theme,version from account_preferences where subject=?",(r,n)->new Preferences(Theme.valueOf(r.getString("theme")),r.getLong("version")),subject).stream().findFirst();
 }
 public boolean updatePreferences(String subject,Theme theme,long version){return db.update("update account_preferences set theme=?,version=version+1 where subject=? and version=?",theme.name(),subject,version)==1;}
 public Optional<Visibility> visibility(String subject){return db.query("select public_id,member_visible,public_visible,privacy_version from profile where subject=?",(r,n)->new Visibility(r.getObject("public_id",UUID.class),r.getBoolean("member_visible"),r.getBoolean("public_visible"),r.getLong("privacy_version")),subject).stream().findFirst();}
 public boolean updateVisibility(String subject,boolean memberVisible,Boolean publicVisible,long version){return db.update("update profile set member_visible=?,public_visible=coalesce(?,public_visible),privacy_version=privacy_version+1 where subject=? and privacy_version=?",memberVisible,publicVisible,subject,version)==1;}
 public Optional<Summary> visibleProfile(UUID id,String subject){return db.query("select name,headline,location,about from profile p where public_id=? and (public_visible=true or (cast(? as text) is not null and member_visible=true) or subject=?) and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=p.subject) or (b.blocked=? and b.blocker=p.subject))",(r,n)->new Summary(r.getString("name"),r.getString("headline"),r.getString("location"),r.getString("about")),id,subject,subject,subject,subject).stream().findFirst();}
}
