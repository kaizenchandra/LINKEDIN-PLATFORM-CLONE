package com.meridian.notifications.domain;
import java.util.*;
public final class Notifications {
 private Notifications(){}
 public enum Category {ALL,NETWORK,JOBS}
 public enum Change {READ,UNREAD,DISMISS}
 public record Notification(UUID id,String actorName,String createdAt,boolean read,String kind,UUID applicationId,UUID jobId,String jobTitle,String applicationStatus,Long applicationVersion){}
 public record Row(Notification notification,long sequence){}
 public record Page(List<Notification> items,String nextCursor,long unreadCount,String readThrough){}
 public record Preference(boolean enabled,boolean jobs,long version){}
 public static long boundary(String value){
  if(value==null||!value.matches("[1-9][0-9]{0,18}"))throw new IllegalArgumentException("Invalid notification boundary");
  try{return Long.parseLong(value);}catch(NumberFormatException invalid){throw new IllegalArgumentException("Invalid notification boundary");}
 }
}
