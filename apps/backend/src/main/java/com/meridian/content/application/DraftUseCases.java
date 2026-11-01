package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
public interface DraftUseCases {
 DraftPage list(String subject,UUID after);
 Draft get(String subject,UUID id);
 Draft save(String subject,UUID id,String text,Audience audience,long version);
 void delete(String subject,UUID id,long version);
 Post publish(String subject,UUID id,long version);
}
