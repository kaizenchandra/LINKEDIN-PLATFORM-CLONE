package com.meridian.messaging.application;
import java.util.*;
import com.meridian.messaging.domain.Messaging.*;
public interface MessagingReadStore {
 List<ConversationRow> list(String subject,Long before,View view,String query);
 Access access(UUID id,String subject);
 List<Message> messages(UUID id,String subject,Long before,Access access);
 Person person(String subject);
 boolean connected(String a,String b);
}
