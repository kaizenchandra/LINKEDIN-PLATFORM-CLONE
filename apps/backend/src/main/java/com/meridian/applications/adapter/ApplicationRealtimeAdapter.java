package com.meridian.applications.adapter;
import com.meridian.applications.application.ApplicationSignals;
import java.util.Set;
import com.meridian.realtime.adapter.RealtimeChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
public class ApplicationRealtimeAdapter implements ApplicationSignals {
 private final ApplicationEventPublisher events;
 public ApplicationRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(Set<String> subjects){events.publishEvent(new RealtimeChanged(subjects));}
}
