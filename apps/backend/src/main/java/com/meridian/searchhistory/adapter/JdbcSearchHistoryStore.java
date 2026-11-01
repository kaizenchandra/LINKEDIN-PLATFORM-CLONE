package com.meridian.searchhistory.adapter;
import com.meridian.searchhistory.application.SearchHistoryStore;
import com.meridian.searchhistory.domain.SearchHistory.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcSearchHistoryStore implements SearchHistoryStore {
 private final JdbcTemplate db;
 public JdbcSearchHistoryStore(JdbcTemplate db){this.db=db;}
 public State state(String subject){
  var preferences=db.query("select enabled,version from search_history_preference where subject=?",(r,n)->new State(r.getBoolean(1),r.getLong(2),List.of()),subject);
  State preference=preferences.isEmpty()?new State(false,0,List.of()):preferences.getFirst();
  var items=preference.enabled()?db.query("select id,kind,query,location,searched_at from search_history where subject=? and searched_at>now()-interval '30 days' order by searched_at desc,id limit 20",(r,n)->new Entry(r.getObject(1,UUID.class),Kind.valueOf(r.getString(2)),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant().toString()),subject):List.<Entry>of();
  return new State(preference.enabled(),preference.version(),items);
 }

 public boolean lockOwner(String subject){return !db.queryForList("select subject from profile where subject=? for update",String.class,subject).isEmpty();}
 public void initialize(String subject){db.update("insert into search_history_preference(subject) values(?) on conflict do nothing",subject);}
 public void purgeOwnerExpired(String subject){db.update("delete from search_history where subject=? and searched_at<=now()-interval '30 days'",subject);}
 public void preference(String subject,boolean enabled){db.update("update search_history_preference set enabled=?,version=version+1 where subject=?",enabled,subject);}
 public void advance(String subject){db.update("update search_history_preference set version=version+1 where subject=?",subject);}
 public void clear(String subject){db.update("delete from search_history where subject=?",subject);}
 public void remove(String subject,UUID id){db.update("delete from search_history where subject=? and id=?",subject,id);}
 public void remember(String subject,Criteria criteria){
  db.update("insert into search_history(id,subject,kind,query,location,query_key,location_key) values(?,?,?,?,?,?,?) on conflict(subject,kind,query_key,location_key) do update set query=excluded.query,location=excluded.location,searched_at=clock_timestamp()",UUID.randomUUID(),subject,criteria.kind().name(),criteria.query(),criteria.location(),criteria.queryKey(),criteria.locationKey());
  db.update("delete from search_history where subject=? and id not in(select id from search_history where subject=? order by searched_at desc,id limit 20)",subject,subject);
 }
 public int purgeExpired(){return db.update(connection->{var statement=connection.prepareStatement("delete from search_history where id in(select id from search_history where searched_at<=now()-interval '30 days' limit 1000 for update skip locked)");statement.setQueryTimeout(5);return statement;});}
}
