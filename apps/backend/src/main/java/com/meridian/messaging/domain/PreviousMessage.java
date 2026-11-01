package com.meridian.messaging.domain;
import java.util.UUID;
public record PreviousMessage(UUID conversation,String sender,String body){
 public boolean matches(UUID id,String subject,String text){return id.equals(conversation)&&subject.equals(sender)&&text.equals(body);}
}
