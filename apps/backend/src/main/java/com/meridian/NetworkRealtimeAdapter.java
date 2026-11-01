package com.meridian;
import com.meridian.network.application.NetworkSignals;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
class NetworkRealtimeAdapter implements NetworkSignals {
 private final ApplicationEventPublisher events;
 NetworkRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(Set<String> subjects){events.publishEvent(new RealtimeChanged(subjects));}
}
