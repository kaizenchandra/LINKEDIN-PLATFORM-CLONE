package com.meridian.messaging.application;
import java.util.UUID;
import com.meridian.messaging.domain.Messaging.*;
public interface MessagingReadUseCases {Conversations list(String subject,Long before,View view,String query);Messages history(String subject,UUID id,Long before);}
