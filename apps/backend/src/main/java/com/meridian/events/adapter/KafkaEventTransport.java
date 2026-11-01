package com.meridian.events.adapter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import jakarta.annotation.PreDestroy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.events.application.OutboxPublisher;
import com.meridian.events.domain.OutboxEvent;
import com.meridian.notifications.adapter.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="meridian.events.enabled",havingValue="true")
public class KafkaEventTransport implements OutboxPublisher {
 private final KafkaProducer<String,String> producer;private final ObjectMapper json;
 public KafkaEventTransport(ObjectMapper json,@Value("${meridian.events.bootstrap}") String bootstrap){
  this.json=json;producer=new KafkaProducer<>(Map.of("bootstrap.servers",bootstrap,"acks","all","enable.idempotence",true,"max.block.ms",2000,"request.timeout.ms",5000,"delivery.timeout.ms",10000),new StringSerializer(),new StringSerializer());
 }
 public void publish(OutboxEvent event)throws Exception{
  boolean connection=event.connectionId()!=null;
  String payload=json.writeValueAsString(connection?new ConnectionAccepted(event.id(),1,"ConnectionAccepted",event.connectionId()):new ApplicationChanged(event.id(),1,"ApplicationChanged",event.applicationId(),event.applicationVersion()));
  send(connection?ConnectionAccepted.TOPIC:ApplicationChanged.TOPIC,(connection?event.connectionId():event.applicationId()).toString(),payload);
 }
 public void send(String topic,String key,String payload)throws Exception{producer.send(new ProducerRecord<>(topic,key,payload)).get(12,TimeUnit.SECONDS);}
 @PreDestroy public void close(){producer.close(Duration.ofSeconds(5));}
}
