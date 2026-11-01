package com.meridian.notifications.application;
import java.util.UUID;
public interface NotificationProjectionUseCases {
 void connectionAccepted(UUID event,UUID connection);
 void applicationChanged(UUID event,UUID application,long version);
}
