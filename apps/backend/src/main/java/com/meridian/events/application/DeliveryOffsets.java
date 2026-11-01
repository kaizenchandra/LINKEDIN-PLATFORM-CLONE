package com.meridian.events.application;
import com.meridian.events.domain.EventDelivery.Partition;
public interface DeliveryOffsets {void commit(Partition partition,long nextOffset)throws Exception;void seek(Partition partition,long offset);}
