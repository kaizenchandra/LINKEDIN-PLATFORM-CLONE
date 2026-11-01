package com.meridian.notifications.application;
import com.meridian.notifications.domain.Notifications.*;
import java.util.*;
public interface NotificationStore {
 List<Row> list(String subject,Long before,Category category);
 long unreadCount(String subject);
 Long readThrough(String subject);
 boolean change(String subject,UUID id,Change change);
 void readAll(String subject,long through);
 Optional<Preference> preferences(String subject);
 boolean preferences(String subject,boolean enabled,Boolean jobs,long version);
}
