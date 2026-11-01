package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
public interface PostUseCases {
 FeedPage feed(String subject,Long before,FeedScope scope);
 FeedPage saved(String subject,Long before);
 FeedPage search(String subject,String query,Long before);
 FeedPage activity(String subject,UUID authorId,Long before);
 Post get(String subject,UUID id);
 Post save(String subject,UUID id,String text,Audience audience,long version);
 void delete(String subject,UUID id,long version);
 void hidden(String subject,UUID id,boolean hidden);
 Post react(String subject,UUID id,boolean liked);
 Post bookmark(String subject,UUID id,boolean saved);
}
