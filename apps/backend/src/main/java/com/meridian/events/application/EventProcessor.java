package com.meridian.events.application;
import com.meridian.events.domain.EventDelivery;
/** Completes projection or confirmed quarantine before returning. */
public interface EventProcessor {void process(EventDelivery delivery)throws Exception;}
