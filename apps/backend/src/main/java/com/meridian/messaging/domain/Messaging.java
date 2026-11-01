package com.meridian.messaging.domain;
import java.util.*;
public final class Messaging {
 private Messaging(){}
 public record Preference(Boolean starred,Boolean archived,Long version){}
 public record Person(UUID memberId,String name){}
 public enum View {INBOX,ARCHIVED,STARRED}
 public record Conversation(UUID id,UUID memberId,String name,long unreadCount,boolean canSend,Preference preferences){}
 public record ConversationRow(Conversation value,long sequence){}
 public record Conversations(List<Conversation> items,String nextCursor){}
 public record Message(UUID id,long sequence,String text,boolean owned,boolean read,String createdAt){}
 public record Messages(List<Message> items,String nextCursor,long latestSequence,long recipientReadSequence,boolean canSend,UUID memberId,String name,Preference preferences){}
 public record Access(String a,String b,long latest,long readA,long readB,Preference prefsA,Preference prefsB){public Preference preferences(String subject){return subject.equals(a)?prefsA:prefsB;}public String other(String subject){return subject.equals(a)?b:a;}public long recipientRead(String sender){return sender.equals(a)?readB:readA;}}
}
