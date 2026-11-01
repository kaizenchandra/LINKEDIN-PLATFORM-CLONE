package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
public interface DraftStore {
 List<Draft> list(String subject,UUID after);
 Optional<Draft> owned(String subject,UUID id,boolean lock);
 boolean lockProfile(String subject);
 boolean exists(UUID id);
 int count(String subject);
 boolean insert(String subject,UUID id,String text,Audience audience);
 boolean update(String subject,UUID id,String text,Audience audience,long version);
 boolean delete(String subject,UUID id,long version);
 boolean createPost(String subject,Draft draft);
 void markPublished(UUID id);
}
