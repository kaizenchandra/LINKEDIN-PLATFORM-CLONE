package com.meridian.messaging.adapter;
import com.meridian.messaging.application.MessagingSignals;
import java.util.Set;
import com.meridian.realtime.adapter.RealtimeChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
public class MessagingRealtimeAdapter implements MessagingSignals {
 private final ApplicationEventPublisher events;
 public MessagingRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(Set<String> subjects){events.publishEvent(new RealtimeChanged(subjects));}
}
