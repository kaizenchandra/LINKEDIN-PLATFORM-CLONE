package com.meridian.events.application;
import java.util.*;
import com.meridian.events.domain.EventDelivery;
import com.meridian.events.domain.EventDelivery.Partition;
public final class EventConsumptionService implements EventConsumptionUseCases {
 private final EventProcessor processor;
 public EventConsumptionService(EventProcessor processor){this.processor=processor;}
 public void consume(List<EventDelivery> records,DeliveryOffsets offsets)throws Exception{
  Map<Partition,Long> committed=new HashMap<>();
  for(var record:records){var partition=record.source();
   try{processor.process(record);offsets.commit(partition,record.offset()+1);committed.put(partition,record.offset()+1);}
   catch(Exception failure){
    Map<Partition,Long> retry=new HashMap<>();
    for(var pending:records){var target=pending.source();if(pending.offset()>=committed.getOrDefault(target,0L))retry.merge(target,pending.offset(),Math::min);}
    retry.forEach(offsets::seek);throw failure;
   }
  }
 }
}
