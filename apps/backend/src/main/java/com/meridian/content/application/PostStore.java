package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
public interface PostStore {
 List<PostRow> list(String subject,Long before,boolean savedOnly,boolean followedOnly,boolean hiddenOnly);
 List<PostRow> search(String subject,String query,Long before);
 List<PostRow> activity(String subject,UUID authorId,Long before);
 Optional<Post> read(String subject,UUID id);
 void insert(String subject,UUID id,String text,Audience audience);
 boolean matching(String subject,UUID id,String text,Audience audience);
 boolean update(String subject,UUID id,String text,Audience audience,long version);
 boolean delete(String subject,UUID id,long version);
 boolean profileExists(String subject);
 void marker(String subject,UUID id,Marker marker,boolean enabled);
}
