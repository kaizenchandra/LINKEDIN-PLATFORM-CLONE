package com.meridian.network.adapter;
import com.meridian.network.application.NetworkSignals;
import java.util.Set;
import com.meridian.realtime.adapter.RealtimeChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
public class NetworkRealtimeAdapter implements NetworkSignals {
 private final ApplicationEventPublisher events;
 public NetworkRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(Set<String> subjects){events.publishEvent(new RealtimeChanged(subjects));}
}
