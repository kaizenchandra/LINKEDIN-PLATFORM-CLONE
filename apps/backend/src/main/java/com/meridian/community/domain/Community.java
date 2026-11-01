package com.meridian.community.domain;
import java.time.Instant;
import java.util.UUID;
public final class Community {
 private Community(){}
 public enum Kind { GROUP, EVENT, NEWSLETTER }
 public record Entry(UUID id,Kind kind,String title,String description,Instant startsAt,String location,boolean joined,long members,boolean owned){}
 public record Update(UUID id,UUID memberId,String name,String text,Instant createdAt){}
 public record Draft(UUID id,Kind kind,String title,String description,Instant startsAt,String location){}
}
