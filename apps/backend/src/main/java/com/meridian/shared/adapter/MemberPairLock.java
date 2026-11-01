package com.meridian.shared.adapter;
import org.springframework.jdbc.core.JdbcTemplate;
/** Transaction-scoped lock shared by invitation and block writes. Hash collisions only serialize unrelated pairs. */
public final class MemberPairLock {
 public static void acquire(JdbcTemplate db,String first,String second){
  db.queryForList("select pg_advisory_xact_lock(hashtextextended(least(?,?) || ':' || greatest(?,?),0))",first,second,first,second);
 }
}
