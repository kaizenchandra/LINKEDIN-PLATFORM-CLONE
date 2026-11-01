package com.meridian.events.adapter;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.events.application.EventProcessor;
import com.meridian.events.domain.EventDelivery;
import com.meridian.notifications.adapter.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="meridian.events.enabled",havingValue="true")
public class NotificationEventProcessor implements EventProcessor {
 private final ObjectMapper json;private final NotificationProjector projector;private final KafkaEventTransport transport;private final MeterRegistry metrics;
 public NotificationEventProcessor(ObjectMapper json,NotificationProjector projector,KafkaEventTransport transport,MeterRegistry metrics){this.json=json;this.projector=projector;this.transport=transport;this.metrics=metrics;}
 public void process(EventDelivery record)throws Exception{
    Object event;boolean application=record.topic().equals(ApplicationChanged.TOPIC);
    try{
     if(application){var tree=json.readTree(record.value());if(tree==null||!tree.path("schemaVersion").isIntegralNumber()||!tree.path("applicationVersion").isIntegralNumber()||!tree.path("applicationVersion").canConvertToLong()||!tree.path("schemaVersion").canConvertToInt())throw new IllegalArgumentException();var value=json.readerFor(ApplicationChanged.class).with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(record.value());if(!(value instanceof ApplicationChanged parsed))throw new IllegalArgumentException();parsed.validate();event=parsed;}
     else{var value=json.readerFor(ConnectionAccepted.class).with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(record.value());if(!(value instanceof ConnectionAccepted parsed))throw new IllegalArgumentException();parsed.validate();event=parsed;}
    }catch(Exception invalid){
     // Never forward untrusted application payloads, which could contain private data, into the DLQ.
     String poison=application?json.writeValueAsString(Map.of("sourceTopic",record.topic(),"partition",record.partition(),"offset",record.offset(),"error","invalid-envelope")):record.value();
     transport.send(application?ApplicationChanged.DLQ:ConnectionAccepted.DLQ,application?record.partition()+":"+record.offset():record.key(),poison);
     metrics.counter("meridian.events.quarantined").increment();return;
    }
    if(event instanceof ApplicationChanged changed)projector.project(changed);else projector.project((ConnectionAccepted)event);
 }
}
