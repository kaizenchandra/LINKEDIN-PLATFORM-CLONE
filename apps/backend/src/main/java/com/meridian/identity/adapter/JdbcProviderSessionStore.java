package com.meridian.identity.adapter;
import com.meridian.identity.application.ProviderSessionStore;
import com.meridian.identity.domain.Sessions.*;
import java.util.*;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcProviderSessionStore implements ProviderSessionStore {
 private final JdbcTemplate db;
 public JdbcProviderSessionStore(JdbcTemplate db){this.db=db;}
 static final String MATCH="e.issuer=b.issuer and e.client_id=b.client_id and (e.subject is null or e.subject=b.subject) and ((e.provider_sid is not null and e.provider_sid=b.provider_sid) or (e.provider_sid is null and b.token_issued_at<=e.issued_at))";
 public void bind(String sessionId,Claims claims,Provider provider){db.update("insert into oidc_session_binding(session_id,issuer,client_id,subject,provider_sid,token_issued_at) values(?,?,?,?,?,?) on conflict(session_id) do nothing",sessionId,claims.issuer(),provider.clientId(),claims.subject(),claims.sid(),Timestamp.from(claims.issuedAt()));}
 public boolean revoked(String sessionId){return Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from oidc_session_binding b join provider_logout_event e on "+MATCH+" where b.session_id=?)",Boolean.class,sessionId));}
 public boolean recordLogout(Claims claims,Provider provider){return db.update("insert into provider_logout_event(issuer,client_id,token_id,subject,provider_sid,issued_at) values(?,?,?,?,?,?) on conflict do nothing",claims.issuer(),provider.clientId(),claims.tokenId(),claims.subject(),claims.sid(),Timestamp.from(claims.issuedAt()))!=0;}
 public int deleteBoundSessions(Claims claims,Provider provider){return db.update("delete from spring_session s using oidc_session_binding b,provider_logout_event e where s.session_id=b.session_id and "+MATCH+" and e.issuer=? and e.client_id=? and e.token_id=?",claims.issuer(),provider.clientId(),claims.tokenId());}
 public void recordCount(Claims claims,Provider provider,int count){db.update("update provider_logout_event set revoked_count=? where issuer=? and client_id=? and token_id=?",count,claims.issuer(),provider.clientId(),claims.tokenId());}
 public void cleanup(){
  db.update("delete from oidc_session_binding where created_at<now()-interval '8 days'");
  db.update("delete from provider_logout_event where received_at<now()-interval '8 days'");
 }
}
