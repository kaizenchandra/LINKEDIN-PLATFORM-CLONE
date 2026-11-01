package com.meridian.content.domain;
import java.util.*;
public final class Content {
 private Content(){}
 public enum Audience {ONLY_ME,MEMBERS}
 public enum FeedScope {RECENT,FOLLOWING,HIDDEN}
 public enum Marker {HIDDEN,LIKED,SAVED}
 public record Post(UUID id,String authorName,UUID authorId,String text,Audience audience,long version,String createdAt,boolean edited,boolean owned,long reactionCount,boolean liked,boolean saved){}
 public record PostRow(Post post,long sequence){}
 public record FeedPage(List<Post> items,String nextCursor){}
 public record Draft(UUID id,String text,Audience audience,long version,boolean published,String updatedAt){}
 public record DraftPage(List<Draft> items,String nextCursor){}
 public record Comment(UUID id,UUID parentId,String authorName,String text,String createdAt,boolean owned){}
 public record CommentRow(Comment comment,long sequence){}
 public record CommentPage(List<Comment> items,String nextCursor){}
 public static void text(String value,int max,boolean required){if(value==null||value.length()>max||required&&value.isBlank())throw new IllegalArgumentException("Invalid content text");}
}
