package com.meridian.events.application;
import java.util.List;
import com.meridian.events.domain.EventDelivery;
public interface EventConsumptionUseCases {void consume(List<EventDelivery> records,DeliveryOffsets offsets)throws Exception;}
