package com.meridian.notifications.application;
import java.util.*;
import com.meridian.notifications.domain.NotificationSource;
public interface NotificationProjectionStore {
 boolean claim(UUID event);
 Optional<String> insertConnection(UUID event,UUID connection);
 Optional<NotificationSource> source(UUID event,UUID application,long version);
 boolean jobsEnabled(String recipient);
 boolean insertApplication(UUID event,UUID application,long version,String recipient,String actor);
}
