package com.meridian.events.application;
import java.util.*;
import com.meridian.events.domain.OutboxEvent;
public interface OutboxStore {List<OutboxEvent> lockReady();void published(UUID id);void retry(UUID id);}
