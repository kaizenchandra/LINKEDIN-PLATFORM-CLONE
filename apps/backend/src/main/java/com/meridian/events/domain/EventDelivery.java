package com.meridian.events.domain;
public record EventDelivery(String topic,int partition,long offset,String key,String value){
 public record Partition(String topic,int number){}
 public Partition source(){return new Partition(topic,partition);}
}
