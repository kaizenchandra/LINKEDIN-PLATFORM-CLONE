package com.meridian.events.adapter;
import com.meridian.events.application.*;
import com.meridian.shared.application.Transactions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Configuration @ConditionalOnProperty(name="meridian.events.enabled",havingValue="true")
public class EventConfiguration {
 @Bean EventConsumptionUseCases consumption(EventProcessor processor){return new EventConsumptionService(processor);}
 @Bean OutboxUseCases outbox(OutboxStore store,OutboxPublisher publisher,MeterRegistry metrics,Transactions transactions){return new OutboxService(store,publisher,new OutboxMetrics(){public void published(){metrics.counter("meridian.outbox.published").increment();}public void retry(){metrics.counter("meridian.outbox.retries").increment();}},transactions);}
}
