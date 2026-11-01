package com.meridian.network.domain;
import java.util.*;
public final class Network {
 private Network(){}
 public enum Action {
  ACCEPT,IGNORE,WITHDRAW,REMOVE;
  public String target(){return switch(this){case ACCEPT->"ACCEPTED";case IGNORE->"IGNORED";case WITHDRAW->"WITHDRAWN";case REMOVE->"REMOVED";};}
  public String source(){return this==REMOVE?"ACCEPTED":"PENDING";}
  public boolean permits(String actor,Pair pair){return switch(this){case ACCEPT,IGNORE->actor.equals(pair.recipient());case WITHDRAW->actor.equals(pair.sender());case REMOVE->actor.equals(pair.sender())||actor.equals(pair.recipient());};}
 }
 public record Pair(String sender,String recipient){public Set<String> subjects(){return Set.of(sender,recipient);}}
 public record Connection(UUID id,String name,String state,boolean incoming,long version,UUID memberId){}
 public record FollowState(boolean following){}
 public record Member(UUID memberId,String name,boolean visible){}
 public record FollowRow(Member member,long sequence){}
 public record FollowPage(List<Member> items,String nextCursor,long total){}
 public record Block(UUID memberId,String name){}
 public record Sharing(boolean connectionsVisible,long version){}
 public record SuggestedMember(UUID memberId,String name,String headline,String location,long mutualCount){}
 public record Page(List<SuggestedMember> items,String nextCursor){}
}
