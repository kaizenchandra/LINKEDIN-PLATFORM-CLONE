package com.meridian.notifications.adapter;
import com.meridian.notifications.application.NotificationSignals;
import java.util.Set;
import com.meridian.realtime.adapter.RealtimeChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
public class NotificationRealtimeAdapter implements NotificationSignals {
 private final ApplicationEventPublisher events;
 public NotificationRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(String subject){events.publishEvent(new RealtimeChanged(Set.of(subject)));}
}
