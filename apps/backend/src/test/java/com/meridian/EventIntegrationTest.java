package com.meridian;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"meridian.events.enabled=true","spring.security.oauth2.client.provider.keycloak.issuer-uri=","spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost/authorize","spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost/token","spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost/jwks","spring.security.oauth2.client.registration.keycloak.client-secret=test"})
@AutoConfigureMockMvc @Testcontainers @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class EventIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.security.oauth2.client.registration.ClientRegistrationRepository registrations;
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17");
 @Container static KafkaContainer kafka=new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.2"));
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);r.add("meridian.events.bootstrap",kafka::getBootstrapServers);}
 @Autowired JdbcTemplate db;@Autowired MockMvc mvc;
 UUID accept()throws Exception{
  UUID id=UUID.randomUUID();UUID recipient=db.queryForObject("select public_id from profile where subject='event-recipient'",UUID.class);
  mvc.perform(post("/api/v1/network").with(oidcLogin().idToken(t->t.subject("event-sender"))).with(csrf()).contentType("application/json").content("{\"id\":\""+id+"\",\"recipientId\":\""+recipient+"\"}")).andExpect(status().isNoContent());
  mvc.perform(put("/api/v1/network/"+id).with(oidcLogin().idToken(t->t.subject("event-recipient"))).with(csrf()).contentType("application/json").content("{\"action\":\"ACCEPT\",\"version\":0}")).andExpect(status().isNoContent());return id;
 }
 void remove(UUID id)throws Exception{mvc.perform(put("/api/v1/network/"+id).with(oidcLogin().idToken(t->t.subject("event-sender"))).with(csrf()).contentType("application/json").content("{\"action\":\"REMOVE\",\"version\":1}")).andExpect(status().isNoContent());}
 @Test @Timeout(150) void outboxKafkaReplayPreferencesPoisonAndBrokerRecovery()throws Exception{
  for(String subject:List.of("event-sender","event-recipient"))mvc.perform(get("/api/v1/me").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isOk());
  db.update("update profile set member_visible=true where subject='event-recipient'");
  UUID connection=accept();
  await().atMost(Duration.ofSeconds(30)).until(()->db.queryForObject("select count(*) from notification where connection_id is not null",Integer.class)==1);
  UUID eventId=db.queryForObject("select id from event_outbox where connection_id=?",UUID.class,connection);
  await().atMost(Duration.ofSeconds(10)).until(()->db.queryForObject("select published_at is not null from event_outbox where id=?",Boolean.class,eventId));
  try(var producer=new KafkaProducer<String,String>(Map.of("bootstrap.servers",kafka.getBootstrapServers(),"acks","all"),new StringSerializer(),new StringSerializer());var dlq=new KafkaConsumer<String,String>(Map.of("bootstrap.servers",kafka.getBootstrapServers(),"group.id","test-"+UUID.randomUUID(),"auto.offset.reset","earliest","enable.auto.commit",false),new StringDeserializer(),new StringDeserializer())){
   String payload=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(new ConnectionAccepted(eventId,1,"ConnectionAccepted",connection));
   for(int i=0;i<2;i++)producer.send(new ProducerRecord<>(ConnectionAccepted.TOPIC,connection.toString(),payload)).get(10,TimeUnit.SECONDS);
   String poison="poison-"+UUID.randomUUID();dlq.subscribe(List.of(ConnectionAccepted.DLQ));producer.send(new ProducerRecord<>(ConnectionAccepted.TOPIC,poison,"{\"invalid\":true}")).get(10,TimeUnit.SECONDS);
   await().atMost(Duration.ofSeconds(30)).until(()->{for(var record:dlq.poll(Duration.ofMillis(300)))if(poison.equals(record.key()))return true;return false;});
   assertEquals(1,db.queryForObject("select count(*) from notification where id=?",Integer.class,eventId));
   assertEquals(1,db.queryForObject("select count(*) from processed_event where id=?",Integer.class,eventId));
  }
  var sender=oidcLogin().idToken(t->t.subject("event-sender"));var recipient=oidcLogin().idToken(t->t.subject("event-recipient"));
  mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/notifications").with(sender)).andExpect(jsonPath("$.unreadCount").value(1));
  mvc.perform(get("/api/v1/notifications").with(recipient)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/notifications/"+eventId+"/read").with(sender)).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/notifications/"+eventId+"/read").with(recipient).with(csrf())).andExpect(status().isNotFound());
  for(int i=0;i<2;i++)mvc.perform(put("/api/v1/notifications/"+eventId+"/read").with(sender).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notifications").with(sender)).andExpect(jsonPath("$.unreadCount").value(0));
  mvc.perform(put("/api/v1/notifications/preferences").with(sender).with(csrf()).contentType("application/json").content("{\"enabled\":false,\"version\":0}")).andExpect(status().isOk());
  remove(connection);UUID suppressed=accept();
  await().atMost(Duration.ofSeconds(20)).until(()->db.queryForObject("select count(*) from processed_event e join event_outbox o on o.id=e.id where o.connection_id=?",Integer.class,suppressed)==1);
  assertEquals(1,db.queryForObject("select count(*) from notification where connection_id is not null",Integer.class));
  mvc.perform(get("/api/v1/notifications").with(sender)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/notifications/preferences").with(sender).with(csrf()).contentType("application/json").content("{\"enabled\":true,\"version\":0}")).andExpect(status().isConflict());
  mvc.perform(put("/api/v1/notifications/preferences").with(sender).with(csrf()).contentType("application/json").content("{\"enabled\":true,\"version\":1}")).andExpect(status().isOk());
  remove(suppressed);
  var docker=org.testcontainers.DockerClientFactory.instance().client();UUID delayed;
  docker.pauseContainerCmd(kafka.getContainerId()).exec();
  try{delayed=accept();UUID pending=delayed;await().atMost(Duration.ofSeconds(25)).until(()->db.queryForObject("select count(*) from event_outbox where connection_id=? and published_at is null and attempts>0",Integer.class,pending)==1);}
  finally{docker.unpauseContainerCmd(kafka.getContainerId()).exec();}
  UUID recovered=delayed;
  await().atMost(Duration.ofSeconds(40)).until(()->db.queryForObject("select count(*) from notification where connection_id=?",Integer.class,recovered)==1);
 }

 @Test @Timeout(90) void applicationEventsUseRealKafkaDeduplicateAndRedactPoison()throws Exception{
  var json=new com.fasterxml.jackson.databind.ObjectMapper();
  for(String subject:List.of("event-app-owner","event-app-member"))mvc.perform(get("/api/v1/me").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isOk());
  UUID company=UUID.randomUUID(),job=UUID.randomUUID();
  db.update("insert into organization(id,owner_subject,name) values(?,?,'Fictional Event Company')",company,"event-app-owner");
  db.update("insert into job(id,organization_id,title,location,description,workplace,status) values(?,?,'Event role','Remote','Fictional role','REMOTE','OPEN')",job,company);
  var member=oidcLogin().idToken(t->t.subject("event-app-member"));var owner=oidcLogin().idToken(t->t.subject("event-app-owner"));
  String body=json.writeValueAsString(Map.of("statement","Private statement never published","resume",Map.of("fileName","resume.txt","text","Private resume never published")));
  String receipt=mvc.perform(put("/api/v1/jobs/"+job+"/application").with(member).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  UUID application=UUID.fromString(json.readTree(receipt).get("id").asText());
  await().atMost(Duration.ofSeconds(20)).until(()->db.queryForObject("select count(*) from notification where application_id=?",Integer.class,application)==1);
  UUID eventId=db.queryForObject("select id from event_outbox where application_id=? and application_version=1",UUID.class,application);
  mvc.perform(get("/api/v1/notifications?category=JOBS").with(owner)).andExpect(jsonPath("$.items[0].applicationStatus").value("SUBMITTED")).andExpect(jsonPath("$.items[0].kind").value("APPLICATION_UPDATE"));
  mvc.perform(get("/api/v1/notifications?category=NETWORK").with(owner)).andExpect(jsonPath("$.items.length()").value(0)).andExpect(jsonPath("$.unreadCount").value(1));
  try(var producer=new KafkaProducer<String,String>(Map.of("bootstrap.servers",kafka.getBootstrapServers(),"acks","all"),new StringSerializer(),new StringSerializer());var reader=new KafkaConsumer<String,String>(Map.of("bootstrap.servers",kafka.getBootstrapServers(),"group.id","app-test-"+UUID.randomUUID(),"auto.offset.reset","earliest","enable.auto.commit",false),new StringDeserializer(),new StringDeserializer())){
   String envelope=json.writeValueAsString(new ApplicationChanged(eventId,1,"ApplicationChanged",application,1));
   producer.send(new ProducerRecord<>(ApplicationChanged.TOPIC,application.toString(),envelope)).get(10,TimeUnit.SECONDS);
   reader.subscribe(List.of(ApplicationChanged.TOPIC,ApplicationChanged.DLQ));
   producer.send(new ProducerRecord<>(ApplicationChanged.TOPIC,"private-key",json.writeValueAsString(Map.of("resume","private-poison-marker")))).get(10,TimeUnit.SECONDS);
   var seen=new HashSet<String>();
   await().atMost(Duration.ofSeconds(20)).until(()->{for(var record:reader.poll(Duration.ofMillis(300))){
    if(record.topic().equals(ApplicationChanged.DLQ)){assertFalse(record.value().contains("private-poison-marker"));assertFalse(record.key().contains("private-key"));assertEquals("invalid-envelope",json.readTree(record.value()).get("error").asText());seen.add("dlq");}
    else if(record.value().contains(eventId.toString())){assertEquals(5,json.readTree(record.value()).size());assertFalse(record.value().contains("Private"));seen.add("event");}
   }return seen.size()==2;});
  }
  assertEquals(1,db.queryForObject("select count(*) from notification where application_id=?",Integer.class,application));
  mvc.perform(put("/api/v1/notifications/preferences").with(member).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("enabled",true,"jobs",false,"version",0)))).andExpect(status().isOk());
  mvc.perform(put("/api/v1/applications/"+application+"/status").with(owner).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("status","REVIEWING","version",1)))).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(20)).until(()->db.queryForObject("select count(*) from processed_event e join event_outbox o on o.id=e.id where o.application_id=? and o.application_version=2",Integer.class,application)==1);
  assertEquals(1,db.queryForObject("select count(*) from notification where application_id=?",Integer.class,application));
  mvc.perform(put("/api/v1/notifications/preferences").with(member).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("enabled",true,"jobs",true,"version",1)))).andExpect(status().isOk());
  mvc.perform(put("/api/v1/applications/"+application+"/status").with(owner).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("status","ACCEPTED","version",2)))).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(20)).until(()->db.queryForObject("select count(*) from notification where application_id=?",Integer.class,application)==2);
  mvc.perform(get("/api/v1/notifications?category=JOBS").with(member)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].applicationStatus").value("ACCEPTED"));
 }
}
