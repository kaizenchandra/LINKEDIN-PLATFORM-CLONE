package com.meridian.moderation.domain;
import java.util.*;
public final class Moderation {
 private Moderation(){}
 public enum Category {SPAM,HARASSMENT,MISINFORMATION,OTHER}
 public enum Decision {REMOVED,DISMISSED}
 public record Report(UUID id,UUID postId,String category,String state,long version,String createdAt){}
 public record Page(List<Report> items,String nextCursor){}
 public record Item(Report report,String details,String postText,long postVersion,String authorName,boolean unavailable){}
 public record ReviewQueue(List<Item> items,String nextCursor){}
 public record Access(boolean allowed){}
 public record ReviewTarget(UUID postId,String author,String reporter,String state,long version,long postVersion){
  public boolean selfReview(String subject){return subject.equals(author)||subject.equals(reporter);}
  public boolean matchesReplay(Decision decision,long expected){return expected<Long.MAX_VALUE&&version==expected+1&&decision.name().equals(state);}
 }
}
