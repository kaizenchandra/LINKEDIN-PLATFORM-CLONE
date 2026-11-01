package com.meridian.identity.domain;
import java.time.Instant;
import java.util.*;
public final class Sessions {
 private Sessions(){}
 public record SessionRow(UUID id,long creation,long lastAccess,long expiry,boolean current){}
 public record SessionView(UUID id,Instant createdAt,Instant lastActiveAt,Instant expiresAt,boolean current){}
 public enum Revocation {REVOKE_ONE,REVOKE_ALL}
 public record Provider(String issuer,String clientId){}
 /** Only claims from framework-validated tokens may cross this adapter boundary. */
 public record Claims(String issuer,List<String> audience,String subject,String sid,Instant issuedAt,String tokenId){
  public Claims{audience=List.copyOf(audience);}
  public void requireProvider(Provider provider){if(provider==null||!provider.issuer().equals(issuer)||!audience.contains(provider.clientId()))throw new IllegalArgumentException("Unexpected identity provider");}
 }
}
