package com.meridian.recruiting.domain;
import java.util.*;
public final class RecruiterAccess {
 private RecruiterAccess(){}
 public enum Action {ACCEPT,DECLINE,LEAVE,REVOKE}
 public record Access(UUID id,UUID organizationId,String companyName,String memberName,String state,long version,boolean owned){}
 public record Page(List<Access> items,String nextCursor){}
 public record Invitation(UUID organizationId,UUID connectionId){}
 public record Membership(String member,String state,long version){
  public String next(Action action){return switch(action){
   case ACCEPT->state.equals("PENDING")?"ACTIVE":null;
   case DECLINE->state.equals("PENDING")?"DECLINED":null;
   case LEAVE->state.equals("ACTIVE")?"LEFT":null;
   case REVOKE->Set.of("PENDING","ACTIVE").contains(state)?"REVOKED":null;
  };}
 }
}
