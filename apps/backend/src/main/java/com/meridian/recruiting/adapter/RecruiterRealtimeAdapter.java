package com.meridian.recruiting.adapter;
import com.meridian.recruiting.application.RecruiterSignals;
import java.util.Set;
import com.meridian.realtime.adapter.RealtimeChanged;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
/** Bridge to the existing AFTER_COMMIT realtime listener. */
@Component
public class RecruiterRealtimeAdapter implements RecruiterSignals {
 private final ApplicationEventPublisher events;
 public RecruiterRealtimeAdapter(ApplicationEventPublisher events){this.events=events;}
 public void changed(Set<String> subjects){events.publishEvent(new RealtimeChanged(subjects));}
}
