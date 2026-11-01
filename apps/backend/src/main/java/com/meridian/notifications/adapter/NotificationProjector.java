package com.meridian.notifications.adapter;
import com.meridian.notifications.application.NotificationProjectionUseCases;
import org.springframework.stereotype.Component;
/** Inbound integration-event adapter; broker envelopes never supply private audience data. */
@Component
public class NotificationProjector {
 private final NotificationProjectionUseCases projections;
 public NotificationProjector(NotificationProjectionUseCases projections){this.projections=projections;}
 public void project(ConnectionAccepted event){event.validate();projections.connectionAccepted(event.eventId(),event.connectionId());}
 public void project(ApplicationChanged event){event.validate();projections.applicationChanged(event.eventId(),event.applicationId(),event.applicationVersion());}
}
