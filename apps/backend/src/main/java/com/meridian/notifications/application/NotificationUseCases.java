package com.meridian.notifications.application;
import com.meridian.notifications.domain.Notifications.*;
import java.util.*;
public interface NotificationUseCases {
 Page list(String subject,Long before,Category category);
 void change(String subject,UUID id,Change change);
 void readAll(String subject,String through);
 Preference preferences(String subject);
 Preference preferences(String subject,boolean enabled,Boolean jobs,long version);
}
