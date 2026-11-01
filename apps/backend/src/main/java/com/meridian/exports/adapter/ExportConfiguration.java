package com.meridian.exports.adapter;
import com.meridian.exports.application.*;
import com.meridian.shared.application.Transactions;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.*;
@Configuration
public class ExportConfiguration {
 @Bean ExportLifecycleUseCases exportLifecycle(ExportLifecycleStore store,Transactions transactions){return new ExportLifecycleService(store,transactions,java.time.Clock.systemUTC());}
 @Bean ExportQueueUseCases exportQueue(ExportQueueStore store,Transactions transactions){return new ExportQueueService(store,transactions);}
 @Bean ExportGenerationUseCases exportGeneration(ExportQueueUseCases queue,ExportSnapshot snapshots,MeterRegistry metrics){return new ExportGenerationService(queue,snapshots,new ExportMetrics(){public void completed(){metrics.counter("meridian.exports.completed").increment();}public void failed(){metrics.counter("meridian.exports.failures").increment();}});}
}
