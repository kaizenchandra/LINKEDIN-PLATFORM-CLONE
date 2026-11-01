package com.meridian;

import java.util.concurrent.*;
import com.meridian.searchhistory.application.SearchHistoryUseCases;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

@Component
class SearchHistoryRetention {
 private final SearchHistoryUseCases history;private final MeterRegistry metrics;
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("search-history-retention").factory());
 SearchHistoryRetention(SearchHistoryUseCases history,MeterRegistry metrics){this.history=history;this.metrics=metrics;worker.scheduleWithFixedDelay(this::purge,60,60,TimeUnit.SECONDS);}
 void purge(){
  try{int count=history.purgeExpired();if(count>0)metrics.counter("meridian.search.history.expired").increment(count);}
  catch(RuntimeException failure){metrics.counter("meridian.search.history.cleanup.failures").increment();}
 }
 @jakarta.annotation.PreDestroy void close(){worker.shutdownNow();}
}
