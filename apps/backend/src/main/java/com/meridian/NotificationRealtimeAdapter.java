package com.meridian;
import com.meridian.notifications.application.NotificationSignals;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
class NotificationRealtimeAdapter implements NotificationSignals {
 private final ApplicationEventPublisher events;
 NotificationRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(String subject){events.publishEvent(new RealtimeChanged(Set.of(subject)));}
}
