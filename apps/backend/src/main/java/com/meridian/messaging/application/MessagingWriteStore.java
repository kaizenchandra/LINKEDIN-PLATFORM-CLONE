package com.meridian.messaging.application;
import java.util.*;
import com.meridian.messaging.domain.PreviousMessage;
import com.meridian.messaging.domain.Messaging.*;
public interface MessagingWriteStore {
 Optional<String> target(UUID member,String subject);void lockPair(String a,String b);boolean blocked(String a,String b);boolean connected(String a,String b);
 UUID start(String a,String b);
 Access access(UUID id,String subject,boolean lock);
 Optional<PreviousMessage> previous(UUID id);boolean insert(UUID message,UUID conversation,long sequence,String subject,String body);void advance(UUID conversation);Message receipt(UUID message,String subject,Access access);
 boolean preferences(UUID id,boolean first,Preference desired);void read(UUID id,boolean first,long through);
}
