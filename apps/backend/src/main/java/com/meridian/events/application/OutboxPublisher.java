package com.meridian.events.application;
import com.meridian.events.domain.OutboxEvent;
public interface OutboxPublisher {void publish(OutboxEvent event)throws Exception;}
