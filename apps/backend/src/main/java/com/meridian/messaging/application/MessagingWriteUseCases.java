package com.meridian.messaging.application;
import java.util.UUID;
import com.meridian.messaging.domain.Messaging.*;
public interface MessagingWriteUseCases {
 UUID start(String subject,UUID member);Message send(String subject,UUID conversation,UUID message,String text);
 Preference preferences(String subject,UUID conversation,Preference desired);void read(String subject,UUID conversation,long through);
}
