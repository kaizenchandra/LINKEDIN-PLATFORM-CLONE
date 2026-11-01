package com.meridian;
import com.meridian.notifications.adapter.ConnectionAccepted;
import com.meridian.notifications.adapter.ApplicationChanged;
import com.meridian.notifications.adapter.NotificationProjector;
import com.meridian.realtime.adapter.RealtimeChanged;
import com.meridian.realtime.adapter.RealtimeHandler;
import com.meridian.identity.application.SessionUseCases;
import com.meridian.shared.adapter.ApplicationBodyLimit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"meridian.exports.worker-enabled=false","meridian.events.enabled=false","spring.security.oauth2.client.provider.keycloak.issuer-uri=", "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost/authorize", "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost/token", "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost/jwks", "spring.security.oauth2.client.registration.keycloak.client-secret=test"})
@AutoConfigureMockMvc @Testcontainers
class ProfileIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.security.oauth2.client.registration.ClientRegistrationRepository registrations;
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
 @Autowired MockMvc mvc;
 @Autowired AccountExportQueue exportQueue;
 @Autowired AccountDataSnapshot exportSnapshots;
 @Autowired SearchHistoryRetention historyRetention;
 @Autowired SessionUseCases sessionAccess;
 @Test void notificationPreferenceFailureRollsBackWriteAndSuppressesAfterCommitSignal(){
  String subject="hex-notification-owner";database.update("insert into profile(subject,name) values(?,?)",subject,"Notification owner");
  var store=org.mockito.Mockito.spy(new com.meridian.notifications.adapter.JdbcNotificationStore(database));
  org.mockito.Mockito.doThrow(new IllegalStateException("Simulated response read outage")).when(store).preferences(subject);
  var delivered=new java.util.concurrent.atomic.AtomicInteger();
  com.meridian.notifications.application.NotificationSignals signals=owner->org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
   @Override public void afterCommit(){delivered.incrementAndGet();}
  });
  var service=new com.meridian.notifications.application.NotificationService(store,signals,networkTransactions);
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.preferences(subject,false,null,0));
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from notification_preference where subject=?",Integer.class,subject));
  org.junit.jupiter.api.Assertions.assertEquals(0,delivered.get());
  org.mockito.Mockito.doCallRealMethod().when(store).preferences(subject);
  var preference=service.preferences(subject,false,null,0);
  org.junit.jupiter.api.Assertions.assertEquals(new com.meridian.notifications.domain.Notifications.Preference(false,true,1),preference);
  org.junit.jupiter.api.Assertions.assertEquals(1,delivered.get());
 }

 @Test void moderationAuditFailureRollsBackTakedownAndDecision(){
  String author="hex-mod-author",reporter="hex-mod-reporter",operator="hex-mod-operator";
  database.update("insert into profile(subject,name) values(?,?),(?,?),(?,?)",author,"Author",reporter,"Reporter",operator,"Operator");
  database.update("insert into moderation_operator(subject) values(?)",operator);
  var post=java.util.UUID.randomUUID();var report=java.util.UUID.randomUUID();
  database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS')",post,author,"Reported content");
  database.update("insert into content_report(id,reporter,post_id,category,details) values(?,?,?,'SPAM','details')",report,reporter,post);
  var store=org.mockito.Mockito.spy(new com.meridian.moderation.adapter.JdbcModerationStore(database));
  var decision=com.meridian.moderation.domain.Moderation.Decision.REMOVED;
  org.mockito.Mockito.doThrow(new IllegalStateException("Simulated audit outage")).when(store).audit(report,operator,decision,"reason",1);
  var service=new com.meridian.moderation.application.ModerationService(store,new com.meridian.content.adapter.JdbcContentAccess(database),networkTransactions);
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.decide(operator,report,decision,"reason",1,1));
  org.junit.jupiter.api.Assertions.assertTrue(database.queryForObject("select moderated_at is null from content_post where id=?",Boolean.class,post));
  org.junit.jupiter.api.Assertions.assertEquals("OPEN",database.queryForObject("select state from content_report where id=?",String.class,report));
  org.junit.jupiter.api.Assertions.assertEquals(1L,database.queryForObject("select version from content_report where id=?",Long.class,report));
  org.mockito.Mockito.doCallRealMethod().when(store).audit(report,operator,decision,"reason",1);
  service.decide(operator,report,decision,"reason",1,1);service.decide(operator,report,decision,"reason",1,1);
  org.junit.jupiter.api.Assertions.assertFalse(database.queryForObject("select moderated_at is null from content_post where id=?",Boolean.class,post));
  org.junit.jupiter.api.Assertions.assertEquals(2L,database.queryForObject("select version from content_report where id=?",Long.class,report));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from moderation_audit where report_id=?",Integer.class,report));
 }

 @Test void sessionRevocationRollsBackWhenAuditPersistenceFails(){
  String subject="hex-session-owner",cookieId=java.util.UUID.randomUUID().toString();var id=java.util.UUID.randomUUID();long now=System.currentTimeMillis();
  database.update("insert into spring_session values(?,?,?,?,?,?,?)",id.toString(),cookieId,now,now,1800,now+1800000,subject);
  var store=org.mockito.Mockito.spy(new com.meridian.identity.adapter.JdbcSessionStore(database));
  org.mockito.Mockito.doThrow(new IllegalStateException("Simulated audit outage")).when(store).audit(subject,com.meridian.identity.domain.Sessions.Revocation.REVOKE_ONE,1);
  var service=new com.meridian.identity.application.SessionService(store,org.mockito.Mockito.mock(com.meridian.identity.application.ProviderSessionUseCases.class),networkTransactions,new com.meridian.identity.domain.SessionPolicy(java.time.Duration.ofHours(12),java.time.Clock.systemUTC()));
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.revoke(subject,id));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from spring_session where primary_id=?",Integer.class,id.toString()));
  org.mockito.Mockito.doCallRealMethod().when(store).audit(subject,com.meridian.identity.domain.Sessions.Revocation.REVOKE_ONE,1);
  service.revoke(subject,id);service.revoke(subject,id);
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from spring_session where primary_id=?",Integer.class,id.toString()));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from session_revocation_audit where subject=?",Integer.class,subject));
 }

 @Autowired com.meridian.shared.application.Transactions networkTransactions;
 @Test void draftPublicationRollsBackPostWhenDraftStateWriteFails(){
  String subject="hex-draft-owner";var id=java.util.UUID.randomUUID();
  database.update("insert into profile(subject,name) values(?,?)",subject,"Draft owner");
  database.update("insert into post_draft(id,author,body,audience) values(?,?,?,?)",id,subject,"Draft publication","MEMBERS");
  var store=org.mockito.Mockito.spy(new com.meridian.content.adapter.JdbcDraftStore(database));
  var posts=new com.meridian.content.application.PostService(new com.meridian.content.adapter.JdbcPostStore(database),new com.meridian.content.adapter.JdbcContentAccess(database),networkTransactions);
  var service=new com.meridian.content.application.DraftService(store,posts,networkTransactions);
  org.mockito.Mockito.doThrow(new IllegalStateException("Simulated draft update outage")).when(store).markPublished(id);
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.publish(subject,id,1));
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from content_post where id=?",Integer.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(false,database.queryForObject("select published from post_draft where id=?",Boolean.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(1L,database.queryForObject("select version from post_draft where id=?",Long.class,id));
  org.mockito.Mockito.doCallRealMethod().when(store).markPublished(id);
  org.junit.jupiter.api.Assertions.assertEquals(id,service.publish(subject,id,1).id());
  org.junit.jupiter.api.Assertions.assertEquals(id,service.publish(subject,id,1).id());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from content_post where id=?",Integer.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(2L,database.queryForObject("select version from post_draft where id=?",Long.class,id));
 }

 @Test void projectionFailureRollsBackDeduplicationAndAllowsReplay()throws Exception{
  var fixture=chat("projection-rollback");var event=java.util.UUID.randomUUID();var connection=java.util.UUID.fromString(fixture.connection());
  var store=org.mockito.Mockito.spy(new com.meridian.notifications.adapter.JdbcNotificationProjectionStore(database));
  org.mockito.Mockito.doAnswer(call->{call.callRealMethod();throw new IllegalStateException("Failure after notification insert");}).when(store).insertConnection(event,connection);
  var signals=org.mockito.Mockito.mock(com.meridian.notifications.application.NotificationSignals.class);
  var service=new com.meridian.notifications.application.NotificationProjectionService(store,signals,networkTransactions);
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.connectionAccepted(event,connection));
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from processed_event where id=?",Integer.class,event));
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from notification where id=?",Integer.class,event));
  org.mockito.Mockito.verifyNoInteractions(signals);
  org.mockito.Mockito.doCallRealMethod().when(store).insertConnection(event,connection);service.connectionAccepted(event,connection);service.connectionAccepted(event,connection);
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from processed_event where id=?",Integer.class,event));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from notification where id=?",Integer.class,event));
  org.mockito.Mockito.verify(signals).changed("projection-rollback-a");
 }

 @Test void submissionFailureRollsBackApplicationResumeAuditAndOutbox()throws Exception{
  var job=applicationJob("submission-rollback-owner");organizationMember("submission-rollback-member");
  var reads=new com.meridian.applications.adapter.JdbcApplicationReadStore(database);
  var statuses=new com.meridian.applications.adapter.JdbcApplicationStatusStore(database,reads);
  var store=org.mockito.Mockito.spy(new com.meridian.applications.adapter.JdbcApplicationSubmissionStore(database,reads,statuses));
  var created=new java.util.concurrent.atomic.AtomicReference<java.util.UUID>();
  org.mockito.Mockito.doAnswer(call->{created.set(call.getArgument(0));call.callRealMethod();throw new IllegalStateException("Failure after outbox write");}).when(store).auditAndOutbox(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq("submission-rollback-member"));
  var signals=org.mockito.Mockito.mock(com.meridian.applications.application.ApplicationSignals.class);
  var service=new com.meridian.applications.application.ApplicationSubmissionService(store,signals,networkTransactions);
  var input=new com.meridian.applications.domain.Submission.Input("Statement",new com.meridian.applications.domain.Submission.Resume("resume.txt","Resume"),0L,java.util.List.of());
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.submit("submission-rollback-member",job,input));
  org.junit.jupiter.api.Assertions.assertNotNull(created.get());
  for(String table:java.util.List.of("application_resume","application_audit","event_outbox"))org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from "+table+" where application_id=?",Integer.class,created.get()));
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from job_application where id=?",Integer.class,created.get()));
  org.mockito.Mockito.verifyNoInteractions(signals);
 }

 @Test void applicationStatusFailureRollsBackStateAuditAndOutbox()throws Exception{
  var job=applicationJob("status-rollback-owner");var id=java.util.UUID.fromString(submitApplication(job,"status-rollback-applicant"));
  var store=org.mockito.Mockito.spy(new com.meridian.applications.adapter.JdbcApplicationStatusStore(database,new com.meridian.applications.adapter.JdbcApplicationReadStore(database)));
  var beforeAudit=database.queryForObject("select count(*) from application_audit where application_id=?",Integer.class,id);
  var beforeOutbox=database.queryForObject("select count(*) from event_outbox where application_id=?",Integer.class,id);
  org.mockito.Mockito.doAnswer(call->{call.callRealMethod();throw new IllegalStateException("Failure after durable writes");}).when(store).auditAndOutbox(id,2,"status-rollback-owner",com.meridian.applications.domain.Applications.State.SUBMITTED,com.meridian.applications.domain.Applications.State.REVIEWING);
  var signals=org.mockito.Mockito.mock(com.meridian.applications.application.ApplicationSignals.class);
  var service=new com.meridian.applications.application.ApplicationStatusService(store,signals,networkTransactions);
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.change("status-rollback-owner",id,com.meridian.applications.domain.Applications.State.REVIEWING,1));
  org.junit.jupiter.api.Assertions.assertEquals("SUBMITTED",database.queryForObject("select status from job_application where id=?",String.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(1L,database.queryForObject("select version from job_application where id=?",Long.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(beforeAudit,database.queryForObject("select count(*) from application_audit where application_id=?",Integer.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(beforeOutbox,database.queryForObject("select count(*) from event_outbox where application_id=?",Integer.class,id));
  org.mockito.Mockito.verifyNoInteractions(signals);
 }

 @Test void networkAcceptanceRollsBackWhenOutboxWriteFails(){
  String sender="hex-network-sender",recipient="hex-network-recipient";
  database.update("insert into profile(subject,name) values(?,?),(?,?)",sender,"Sender",recipient,"Recipient");
  var id=java.util.UUID.randomUUID();database.update("insert into connection_request(id,sender,recipient) values(?,?,?)",id,sender,recipient);
  var store=org.mockito.Mockito.spy(new com.meridian.network.adapter.JdbcNetworkStore(database));
  org.mockito.Mockito.doThrow(new IllegalStateException("Simulated outbox outage")).when(store).enqueueAccepted(id);
  var delivered=new java.util.concurrent.atomic.AtomicInteger();
  com.meridian.network.application.NetworkSignals signals=subjects->org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
   @Override public void afterCommit(){delivered.incrementAndGet();}
  });
  var service=new com.meridian.network.application.NetworkService(store,networkTransactions,signals);
  org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,()->service.transition(recipient,id,com.meridian.network.domain.Network.Action.ACCEPT,0));
  org.junit.jupiter.api.Assertions.assertEquals("PENDING",database.queryForObject("select state from connection_request where id=?",String.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(0L,database.queryForObject("select version from connection_request where id=?",Long.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(0,delivered.get());
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from event_outbox where connection_id=?",Integer.class,id));
  org.mockito.Mockito.doCallRealMethod().when(store).enqueueAccepted(id);
  service.transition(recipient,id,com.meridian.network.domain.Network.Action.ACCEPT,0);
  service.transition(recipient,id,com.meridian.network.domain.Network.Action.ACCEPT,0);
  org.junit.jupiter.api.Assertions.assertEquals("ACCEPTED",database.queryForObject("select state from connection_request where id=?",String.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(1,delivered.get());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from event_outbox where connection_id=?",Integer.class,id));
 }

 @Test void searchHistoryIsOptInPrivateBoundedAndNormalized()throws Exception{
  var owner=oidcLogin().idToken(t->t.subject("history-owner"));var other=oidcLogin().idToken(t->t.subject("history-other"));String path="/api/v1/me/search-history";
  mvc.perform(get("/api/v1/me").with(owner));mvc.perform(get("/api/v1/me").with(other));
  mvc.perform(get(path)).andExpect(status().isUnauthorized());mvc.perform(get(path).with(owner)).andExpect(jsonPath("$.enabled").value(false)).andExpect(jsonPath("$.version").value(0));
  String write=encodeFixture(java.util.Map.of("kind","people","query","  Fictional  Search  ","location"," Remote ","version",0));
  mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write)).andExpect(status().isConflict());
  mvc.perform(put(path+"/preference").with(owner).contentType("application/json").content("{\"enabled\":true,\"version\":0}")).andExpect(status().isForbidden());
  mvc.perform(put(path+"/preference").with(owner).with(csrf()).contentType("application/json").content("{\"enabled\":true,\"version\":0}")).andExpect(jsonPath("$.version").value(1));
  for(int i=0;i<2;i++)mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write.replace("\"version\":0","\"version\":1"))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].query").value("Fictional Search"));
  mvc.perform(get(path).with(other)).andExpect(jsonPath("$.items.length()").value(0));
  var id=database.queryForObject("select id from search_history where subject='history-owner'",java.util.UUID.class);
  mvc.perform(delete(path+"/entries/"+id+"?version=0").with(other).with(csrf())).andExpect(status().isOk());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from search_history where subject='history-owner'",Integer.class));
  for(int i=0;i<25;i++)mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content("{\"kind\":\"posts\",\"query\":\"Bounded search "+i+"\",\"location\":\"discard me\",\"version\":1}")).andExpect(status().isOk());
  mvc.perform(get(path).with(owner)).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.items.length()").value(20)).andExpect(jsonPath("$.items[0].location").value(""));
  org.junit.jupiter.api.Assertions.assertEquals(20,database.queryForObject("select count(*) from search_history where subject='history-owner'",Integer.class));
 }
 @Test void clearingAndDisablingHistoryRejectOlderRequestsAndRetentionRemovesExpiredQueries()throws Exception{
  String subject="history-privacy",path="/api/v1/me/search-history";var owner=oidcLogin().idToken(t->t.subject(subject));mvc.perform(get("/api/v1/me").with(owner));
  mvc.perform(put(path+"/preference").with(owner).with(csrf()).contentType("application/json").content("{\"enabled\":true,\"version\":0}"));
  String write="{\"kind\":\"people\",\"query\":\"Private history phrase\",\"location\":\"\",\"version\":1}";
  mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write)).andExpect(status().isOk());
  org.junit.jupiter.api.Assertions.assertTrue(new String(exportSnapshots.generate(subject)).contains("Private history phrase"));
  mvc.perform(delete(path+"?version=1").with(owner).with(csrf())).andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write)).andExpect(status().isConflict());
  org.junit.jupiter.api.Assertions.assertFalse(new String(exportSnapshots.generate(subject)).contains("Private history phrase"));
  mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write.replace(":1}",":2}"))).andExpect(status().isOk());
  database.update("update search_history set searched_at=now()-interval '31 days' where subject=?",subject);
  mvc.perform(get(path).with(owner)).andExpect(jsonPath("$.items.length()").value(0));historyRetention.purge();
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from search_history where subject=?",Integer.class,subject));
  mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write.replace(":1}",":2}"))).andExpect(status().isOk());
  mvc.perform(put(path+"/preference").with(owner).with(csrf()).contentType("application/json").content("{\"enabled\":false,\"version\":2}")).andExpect(jsonPath("$.version").value(3)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(path+"/entries").with(owner).with(csrf()).contentType("application/json").content(write.replace(":1}",":2}"))).andExpect(status().isConflict());
  mvc.perform(put(path+"/preference").with(owner).with(csrf()).contentType("application/json").content("{\"enabled\":true,\"version\":3}")).andExpect(jsonPath("$.items.length()").value(0));
 }
 org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor recent(String subject){return oidcLogin().idToken(t->t.subject(subject).claim("auth_time",java.time.Instant.now()));}
 @Test void exportsRequireRecentIdentityAndExcludeOtherMembersPrivateData()throws Exception{
  var fixture=chat("export-private");var owner=recent("export-private-a");var other=recent("export-private-b");var id=java.util.UUID.randomUUID();String url="/api/v1/me/exports/"+id;
  mvc.perform(put("/api/v1/conversations/"+fixture.id()+"/messages/"+java.util.UUID.randomUUID()).with(owner).with(csrf()).contentType("application/json").content("{\"text\":\"Owned export message\"}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/conversations/"+fixture.id()+"/messages/"+java.util.UUID.randomUUID()).with(other).with(csrf()).contentType("application/json").content("{\"text\":\"Other private contribution\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/me/exports")).andExpect(status().isUnauthorized());
  mvc.perform(put(url).with(owner)).andExpect(status().isForbidden());
  mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("export-private-a"))).with(csrf())).andExpect(status().isPreconditionRequired());
  mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("export-private-a").claim("auth_time",java.time.Instant.now().minusSeconds(600)))).with(csrf())).andExpect(status().isPreconditionRequired());
  for(int retry=0;retry<2;retry++)mvc.perform(put(url).with(owner).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUEUED"));
  mvc.perform(get(url+"/download").with(owner)).andExpect(status().isConflict());
  mvc.perform(get(url+"/download").with(other)).andExpect(status().isNotFound());
  mvc.perform(delete(url).with(other).with(csrf())).andExpect(status().isNotFound());
  var work=exportQueue.claim();org.junit.jupiter.api.Assertions.assertEquals(id,work.id());
  org.junit.jupiter.api.Assertions.assertTrue(exportQueue.complete(work,exportSnapshots.generate(work.subject())));
  var response=mvc.perform(get(url+"/download").with(owner)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff")).andReturn().getResponse();
  var archive=response.getContentAsString();org.junit.jupiter.api.Assertions.assertTrue(archive.contains("Owned export message"));org.junit.jupiter.api.Assertions.assertFalse(archive.contains("Other private contribution"));org.junit.jupiter.api.Assertions.assertFalse(archive.contains("SPRING_SECURITY_CONTEXT"));
  mvc.perform(put("/api/v1/me/exports/"+java.util.UUID.randomUUID()).with(owner).with(csrf())).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","600"));
  mvc.perform(delete(url).with(owner).with(csrf())).andExpect(status().isNoContent());mvc.perform(delete(url).with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(url+"/download").with(owner)).andExpect(status().isGone());
  org.junit.jupiter.api.Assertions.assertNull(database.queryForObject("select payload from account_export where id=?",byte[].class,id));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from account_export_audit where export_id=? and action='CANCELED'",Integer.class,id));
 }
 @Test void exportLeasesRecoverAndCancellationPreventsLatePublication()throws Exception{
  String subject="export-lease";var user=recent(subject);mvc.perform(get("/api/v1/me").with(user));var id=java.util.UUID.randomUUID();String url="/api/v1/me/exports/"+id;
  mvc.perform(put(url).with(user).with(csrf())).andExpect(status().isOk());
  var first=exportQueue.claim();org.junit.jupiter.api.Assertions.assertNull(exportQueue.claim());
  database.update("update account_export set lease_until=now()-interval '1 second' where id=?",id);
  var second=exportQueue.claim();org.junit.jupiter.api.Assertions.assertEquals(2,second.attempt());
  org.junit.jupiter.api.Assertions.assertFalse(exportQueue.complete(first,"{}".getBytes()));
  mvc.perform(delete(url).with(user).with(csrf())).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertFalse(exportQueue.complete(second,"{}".getBytes()));
  mvc.perform(get(url+"/download").with(user)).andExpect(status().isGone());
  // Expiry is enforced at the endpoint even before the cleanup worker runs.
  database.update("update account_export set status='READY',payload=?,expires_at=now()-interval '1 second' where id=?","{}".getBytes(),id);
  mvc.perform(get(url+"/download").with(user)).andExpect(status().isGone());exportQueue.claim();
  org.junit.jupiter.api.Assertions.assertNull(database.queryForObject("select payload from account_export where id=?",byte[].class,id));
 }
 @Test void oversizedExportsFailWithoutReturningPartialArchives()throws Exception{
  String subject="export-large";var user=recent(subject);mvc.perform(get("/api/v1/me").with(user));
  database.update("insert into content_post(id,author,body,audience) select gen_random_uuid(),?,repeat('x',3000),'ONLY_ME' from generate_series(1,4000)",subject);
  var id=java.util.UUID.randomUUID();mvc.perform(put("/api/v1/me/exports/"+id).with(user).with(csrf())).andExpect(status().isOk());
  new AccountExportWorker(exportQueue,exportSnapshots,new io.micrometer.core.instrument.simple.SimpleMeterRegistry()).tick();
  mvc.perform(get("/api/v1/me/exports").with(user)).andExpect(jsonPath("$[0].status").value("FAILED")).andExpect(jsonPath("$[0].failure").value("TOO_LARGE"));
  org.junit.jupiter.api.Assertions.assertNull(database.queryForObject("select payload from account_export where id=?",byte[].class,id));
 }





 String questionForm(long version,java.util.UUID id,String prompt,boolean required){return encodeFixture(new com.meridian.jobs.domain.JobQuestions.Form(version,java.util.List.of(new com.meridian.jobs.domain.JobQuestions.Question(id,prompt,required))));}
 String questionSubmission(long version,java.util.UUID id,String text){return encodeFixture(java.util.Map.of("statement","Question application","questionVersion",version,"answers",java.util.List.of(new com.meridian.applications.domain.Submission.Answer(id,text))));}
 @Test void jobQuestionFormsRequireCurrentRecruitingAuthorityAndVersion()throws Exception{
  var pair=chat("question-role");var job=applicationJob("question-role-a");var company=database.queryForObject("select organization_id from job where id=?",java.util.UUID.class,job);var question=java.util.UUID.randomUUID();String url="/api/v1/jobs/"+job+"/questions";
  var owner=oidcLogin().idToken(t->t.subject("question-role-a"));var member=oidcLogin().idToken(t->t.subject("question-role-b"));
  mvc.perform(get(url)).andExpect(status().isUnauthorized());
  mvc.perform(get(url).with(member)).andExpect(jsonPath("$.version").value(0)).andExpect(jsonPath("$.questions.length()").value(0));
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(questionForm(0,question,"A question",true))).andExpect(status().isForbidden());
  mvc.perform(put(url).with(owner).contentType("application/json").content(questionForm(0,question,"A question",true))).andExpect(status().isForbidden());
  var q=new com.meridian.jobs.domain.JobQuestions.Question(question,"A question",true);
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(encodeFixture(new com.meridian.jobs.domain.JobQuestions.Form(0L,java.util.List.of(q,q))))).andExpect(status().isBadRequest());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(questionForm(0,question," ",true))).andExpect(status().isBadRequest());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(questionForm(0,question,"  Describe your experience  ",true))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.questions[0].prompt").value("Describe your experience"));
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(questionForm(0,question,"Stale overwrite",true))).andExpect(status().isConflict());
  var grant=java.util.UUID.randomUUID();String invitation="/api/v1/organizations/"+company+"/recruiter-invitations/"+grant;
  mvc.perform(put(invitation).with(owner).with(csrf()).contentType("application/json").content(encodeFixture(java.util.Map.of("connectionId",pair.connection())))).andExpect(status().isOk());
  mvc.perform(put("/api/v1/recruiter-access/"+grant).with(member).with(csrf()).contentType("application/json").content(recruiterChange("ACCEPT",1))).andExpect(status().isOk());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(questionForm(1,question,"Recruiter question",false))).andExpect(status().isOk());
  mvc.perform(put("/api/v1/recruiter-access/"+grant).with(owner).with(csrf()).contentType("application/json").content(recruiterChange("REVOKE",2))).andExpect(status().isOk());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(questionForm(2,question,"Unauthorized",true))).andExpect(status().isForbidden());
 }
 @Test void applicationAnswersAreValidatedPrivateImmutableAndRetryable()throws Exception{
  var job=applicationJob("questions-owner");organizationMember("questions-member");var q=java.util.UUID.randomUUID();String form="/api/v1/jobs/"+job+"/questions",apply="/api/v1/jobs/"+job+"/application";
  var owner=oidcLogin().idToken(t->t.subject("questions-owner"));var member=oidcLogin().idToken(t->t.subject("questions-member"));var outsider=oidcLogin().idToken(t->t.subject("questions-outsider"));
  mvc.perform(put(form).with(owner).with(csrf()).contentType("application/json").content(questionForm(0,q,"Original question",true))).andExpect(status().isOk());
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(questionSubmission(0,q,"Answer"))).andExpect(status().isConflict());
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(questionSubmission(1,q," "))).andExpect(status().isUnprocessableEntity());
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(questionSubmission(1,java.util.UUID.randomUUID(),"Answer"))).andExpect(status().isBadRequest());
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(questionSubmission(1,q,"x".repeat(1001)))).andExpect(status().isBadRequest());
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(encodeFixture(java.util.Map.of("statement","Question application","questionVersion",1,"answers",java.util.List.of(new com.meridian.applications.domain.Submission.Answer(q,"One"),new com.meridian.applications.domain.Submission.Answer(q,"Two")))))).andExpect(status().isBadRequest());
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from job_application where job_id=?",Integer.class,job));
  String body=questionSubmission(1,q,"  Private answer  ");
  String receipt=mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.answers[0].text").value("Private answer")).andReturn().getResponse().getContentAsString();
  String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(receipt).get("id").asText();
  mvc.perform(put(form).with(owner).with(csrf()).contentType("application/json").content(questionForm(1,q,"Changed question",false))).andExpect(status().isOk());
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.answers[0].prompt").value("Original question")).andExpect(jsonPath("$.answers[0].required").value(true));
  mvc.perform(put(apply).with(member).with(csrf()).contentType("application/json").content(questionSubmission(1,q,"Different"))).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/applications?job="+job).with(outsider)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/applications?view=RECEIVED&job="+job).with(owner)).andExpect(jsonPath("$.items[0].answers[0].text").value("Private answer"));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from event_outbox where application_id=?",Integer.class,java.util.UUID.fromString(id)));
 }
 @Test void questionEditsAndApplicationSubmissionSerialize()throws Exception{
  var job=applicationJob("question-race-owner");organizationMember("question-race-member");var q=java.util.UUID.randomUUID();String form="/api/v1/jobs/"+job+"/questions",apply="/api/v1/jobs/"+job+"/application";
  mvc.perform(put(form).with(oidcLogin().idToken(t->t.subject("question-race-owner"))).with(csrf()).contentType("application/json").content(questionForm(0,q,"Original",true))).andExpect(status().isOk());
  var start=new java.util.concurrent.CountDownLatch(1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var edit=pool.submit(()->{start.await();return mvc.perform(put(form).with(oidcLogin().idToken(t->t.subject("question-race-owner"))).with(csrf()).contentType("application/json").content(questionForm(1,q,"Changed",true))).andReturn().getResponse().getStatus();});
   var submit=pool.submit(()->{start.await();return mvc.perform(put(apply).with(oidcLogin().idToken(t->t.subject("question-race-member"))).with(csrf()).contentType("application/json").content(questionSubmission(1,q,"Original answer"))).andReturn().getResponse().getStatus();});
   start.countDown();org.junit.jupiter.api.Assertions.assertEquals(200,edit.get());int result=submit.get();org.junit.jupiter.api.Assertions.assertTrue(result==200||result==409);
   if(result==200)org.junit.jupiter.api.Assertions.assertEquals("Original",database.queryForObject("select answers->0->>'prompt' from job_application where job_id=?",String.class,job));
   else org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from job_application where job_id=?",Integer.class,job));
  }
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void applicationNotificationProjectionValidatesSourceAndKeepsPreferencesIndependent()throws Exception{
  var job=applicationJob("app-notify-owner");String id=submitApplication(job,"app-notify-member");
  var application=java.util.UUID.fromString(id);
  var eventId=database.queryForObject("select id from event_outbox where application_id=? and application_version=1",java.util.UUID.class,application);
  var events=org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);var projector=projectionFixture(events);
  projector.project(new ApplicationChanged(eventId,1,"ApplicationChanged",application,2));
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from processed_event where id=?",Integer.class,eventId));
  var event=new ApplicationChanged(eventId,1,"ApplicationChanged",application,1);projector.project(event);projector.project(event);
  org.mockito.Mockito.verify(events,org.mockito.Mockito.times(1)).publishEvent(new RealtimeChanged(java.util.Set.of("app-notify-owner")));
  var owner=oidcLogin().idToken(t->t.subject("app-notify-owner"));var member=oidcLogin().idToken(t->t.subject("app-notify-member"));
  mvc.perform(get("/api/v1/notifications?category=JOBS").with(owner)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].applicationVersion").value(1)).andExpect(jsonPath("$.items[0].applicationStatus").value("SUBMITTED")).andExpect(jsonPath("$.items[0].statement").doesNotExist()).andExpect(jsonPath("$.items[0].resume").doesNotExist());
  mvc.perform(get("/api/v1/notifications?category=JOBS").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/notifications/"+eventId+"/read").with(member).with(csrf())).andExpect(status().isNotFound());
  var json=new com.fasterxml.jackson.databind.ObjectMapper();
  mvc.perform(put("/api/v1/notifications/preferences").with(owner).with(csrf()).contentType("application/json").content(json.writeValueAsString(java.util.Map.of("enabled",true,"jobs",false,"version",0)))).andExpect(status().isOk()).andExpect(jsonPath("$.jobs").value(false));
  mvc.perform(put("/api/v1/notifications/preferences").with(owner).with(csrf()).contentType("application/json").content(json.writeValueAsString(java.util.Map.of("enabled",false,"version",1)))).andExpect(status().isOk()).andExpect(jsonPath("$.jobs").value(false)).andExpect(jsonPath("$.enabled").value(false));
  mvc.perform(put("/api/v1/notifications/preferences").with(owner).with(csrf()).contentType("application/json").content(json.writeValueAsString(java.util.Map.of("enabled",true,"jobs",true,"version",1)))).andExpect(status().isConflict());
  submitApplication(job,"app-notify-member");
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from event_outbox where application_id=?",Integer.class,application));
  mvc.perform(put("/api/v1/applications/"+id+"/status").with(member).with(csrf()).contentType("application/json").content(json.writeValueAsString(java.util.Map.of("status","WITHDRAWN","version",1)))).andExpect(status().isOk());
  var withdrawn=database.queryForObject("select id from event_outbox where application_id=? and application_version=2",java.util.UUID.class,application);
  projector.project(new ApplicationChanged(withdrawn,1,"ApplicationChanged",application,2));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from notification where application_id=?",Integer.class,application));
  org.mockito.Mockito.verifyNoMoreInteractions(events);
 }
 String resumeSubmission(String fileName,String text)throws Exception{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("statement","Resume application","resume",java.util.Map.of("fileName",fileName,"text",text)));}
 @Test void applicationResumesPersistAtomicallyRetryAndAuthorizeDownloads()throws Exception{
  var job=applicationJob("resume-owner");organizationMember("resume-member");
  var owner=oidcLogin().idToken(t->t.subject("resume-owner"));var member=oidcLogin().idToken(t->t.subject("resume-member"));var outsider=oidcLogin().idToken(t->t.subject("resume-outsider"));
  String url="/api/v1/jobs/"+job+"/application",text="Fictional engineer — 技能: Java"+System.lineSeparator()+"<script>not executed</script>",body=resumeSubmission("My resume.txt",text);
  mvc.perform(put(url).with(member).contentType("application/json").content(body)).andExpect(status().isForbidden());
  String response=mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.hasResume").value(true)).andReturn().getResponse().getContentAsString();
  String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText(),download="/api/v1/applications/"+id+"/resume";
  org.junit.jupiter.api.Assertions.assertFalse(response.contains("not executed"));
  mvc.perform(get(download)).andExpect(status().isUnauthorized());
  mvc.perform(get(download).with(outsider)).andExpect(status().isNotFound());
  mvc.perform(get(download).with(member)).andExpect(status().isOk()).andExpect(content().bytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))).andExpect(header().string("Content-Type","text/plain;charset=UTF-8")).andExpect(header().string("Content-Disposition","attachment; filename=resume.txt")).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Content-Security-Policy","default-src 'none'; sandbox"));
  mvc.perform(get(download).with(owner)).andExpect(status().isOk());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(resumeSubmission("renamed.txt",text))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(resumeSubmission("My resume.txt","Changed"))).andExpect(status().isConflict());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("statement","Resume application")))).andExpect(status().isConflict());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from application_resume where application_id=?",Integer.class,java.util.UUID.fromString(id)));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from application_audit where application_id=?",Integer.class,java.util.UUID.fromString(id)));
  String without=submitApplication(job,"resume-without");
  mvc.perform(get("/api/v1/applications/"+without+"/resume").with(owner)).andExpect(status().isNotFound());
 }
 @Test void invalidResumeNeverCreatesApplicationAndRequestBodiesAreBounded()throws Exception{
  var job=applicationJob("resume-invalid-owner");organizationMember("resume-invalid-member");var member=oidcLogin().idToken(t->t.subject("resume-invalid-member"));String url="/api/v1/jobs/"+job+"/application";
  for(var file:java.util.List.of("cv.pdf","../cv.txt","folder"+(char)92+"cv.txt","cv"+(char)10+".txt")){
   mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(resumeSubmission(file,"Valid text"))).andExpect(status().isBadRequest());
  }
  for(var text:java.util.List.of(" ","x"+(char)0,"x"+(char)27,"x"+(char)0xD800,"é".repeat(10241),"x".repeat(20481))){
   mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(resumeSubmission("resume.txt",text).replace(String.valueOf((char)0xD800),String.valueOf((char)92)+"uD800"))).andExpect(status().isBadRequest());
  }
  // Missing nested required text must produce validation failure, never a null-pointer failure.
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("statement","Valid","resume",java.util.Map.of("fileName","resume.txt"))))).andExpect(status().isBadRequest());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(" ".repeat(ApplicationBodyLimit.LIMIT+1))).andExpect(status().isPayloadTooLarge());
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from job_application where job_id=?",Integer.class,job));
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(resumeSubmission("resume.txt","é".repeat(10240)))).andExpect(status().isOk());
 }

 @Test void applicationBodyLimitChecksUndeclaredLengthWithoutCallingController()throws Exception{
  var request=new org.springframework.mock.web.MockHttpServletRequest("PUT","/api/v1/jobs/123/application"){
   @Override public long getContentLengthLong(){return -1;}
   @Override public int getContentLength(){return -1;}
  };
  request.setContent(" ".repeat(ApplicationBodyLimit.LIMIT+1).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  var response=new org.springframework.mock.web.MockHttpServletResponse();var chain=new org.springframework.mock.web.MockFilterChain();
  new ApplicationBodyLimit().doFilter(request,response,chain);
  org.junit.jupiter.api.Assertions.assertEquals(413,response.getStatus());
  org.junit.jupiter.api.Assertions.assertNull(chain.getRequest());
 }
 String recruiterChange(String action,long version)throws Exception{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("action",action,"version",version));}
 @Test void recruiterAcceptanceAndRevocationEnforcePrivateApplicationAccess()throws Exception{
  var pair=chat("recruiter");var job=applicationJob("recruiter-a");var company=database.queryForObject("select organization_id from job where id=?",java.util.UUID.class,job);
  String application=submitApplication(job,"recruiter-candidate"),id=java.util.UUID.randomUUID().toString(),invite="/api/v1/organizations/"+company+"/recruiter-invitations/"+id,access="/api/v1/recruiter-access/"+id,history="/api/v1/applications/"+application+"/history",review="/api/v1/applications/"+application+"/status";
  var owner=oidcLogin().idToken(t->t.subject("recruiter-a"));var recruiter=oidcLogin().idToken(t->t.subject("recruiter-b"));
  String body=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("connectionId",pair.connection()));
  mvc.perform(put(invite).with(recruiter).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(put(invite).with(owner).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(put(invite).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("PENDING"));
  mvc.perform(put(invite).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(get(history).with(recruiter)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/jobs/"+job).with(recruiter)).andExpect(jsonPath("$.canManage").value(false));
  mvc.perform(put(access).with(owner).with(csrf()).contentType("application/json").content(recruiterChange("ACCEPT",1))).andExpect(status().isForbidden());
  mvc.perform(put(access).with(recruiter).with(csrf()).contentType("application/json").content(recruiterChange("ACCEPT",1))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("ACTIVE"));
  mvc.perform(get("/api/v1/organizations/"+company).with(recruiter)).andExpect(jsonPath("$.owned").value(false)).andExpect(jsonPath("$.canRecruit").value(true));
  mvc.perform(put("/api/v1/jobs/"+job).with(recruiter).with(csrf()).contentType("application/json").content(jobBody(company,"Recruiter role","OPEN",1))).andExpect(status().isOk()).andExpect(jsonPath("$.canManage").value(true));
  String reviewing=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("status","REVIEWING","version",1));
  mvc.perform(put(review).with(recruiter).with(csrf()).contentType("application/json").content(reviewing)).andExpect(status().isOk());
  mvc.perform(get(history).with(recruiter)).andExpect(jsonPath("$.items[1].actorRole").value("RECRUITER"));
  mvc.perform(get("/api/v1/applications?view=RECEIVED&job="+job).with(recruiter)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(put(access).with(owner).with(csrf()).contentType("application/json").content(recruiterChange("REVOKE",1))).andExpect(status().isConflict());
  mvc.perform(put(access).with(owner).with(csrf()).contentType("application/json").content(recruiterChange("REVOKE",2))).andExpect(status().isOk());
  mvc.perform(get(history).with(recruiter)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/applications?view=RECEIVED&job="+job).with(recruiter)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(review).with(recruiter).with(csrf()).contentType("application/json").content(reviewing)).andExpect(status().isNotFound());
  mvc.perform(put("/api/v1/jobs/"+job).with(recruiter).with(csrf()).contentType("application/json").content(jobBody(company,"Unauthorized","OPEN",2))).andExpect(status().isForbidden());
  mvc.perform(put(access).with(recruiter).with(csrf()).contentType("application/json").content(recruiterChange("ACCEPT",3))).andExpect(status().isConflict());
  org.junit.jupiter.api.Assertions.assertEquals(3,database.queryForObject("select count(*) from recruiter_access_audit where access_id=?",Integer.class,java.util.UUID.fromString(id)));
 }

 @Test void recruiterInvitationUniquenessLeavingAndSelfReviewAreEnforced()throws Exception{
  var pair=chat("recruiter-leave");var job=applicationJob("recruiter-leave-a");
  var company=database.queryForObject("select organization_id from job where id=?",java.util.UUID.class,job);
  String ownApplication=submitApplication(job,"recruiter-leave-b"),id=java.util.UUID.randomUUID().toString(),url="/api/v1/organizations/"+company+"/recruiter-invitations/";
  var owner=oidcLogin().idToken(t->t.subject("recruiter-leave-a"));var recruiter=oidcLogin().idToken(t->t.subject("recruiter-leave-b"));
  String body=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("connectionId",pair.connection()));
  mvc.perform(put(url+id).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(put(url+java.util.UUID.randomUUID()).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
  mvc.perform(put("/api/v1/recruiter-access/"+id).with(recruiter).with(csrf()).contentType("application/json").content(recruiterChange("ACCEPT",1))).andExpect(status().isOk());
  String review=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("status","REVIEWING","version",1));
  mvc.perform(put("/api/v1/applications/"+ownApplication+"/status").with(recruiter).with(csrf()).contentType("application/json").content(review)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/recruiter-access?organization="+company).with(recruiter)).andExpect(status().isForbidden());
  mvc.perform(put(url+java.util.UUID.randomUUID()).with(recruiter).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/recruiter-access/"+id).with(recruiter).with(csrf()).contentType("application/json").content(recruiterChange("LEAVE",2))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("LEFT"));
  mvc.perform(get("/api/v1/jobs/"+job).with(recruiter)).andExpect(jsonPath("$.canManage").value(false));
  String second=java.util.UUID.randomUUID().toString();
  mvc.perform(put(url+second).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(put("/api/v1/recruiter-access/"+second).with(recruiter).with(csrf()).contentType("application/json").content(recruiterChange("DECLINE",1))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("DECLINED"));
  mvc.perform(put("/api/v1/recruiter-access/"+second).with(recruiter).with(csrf()).contentType("application/json").content(recruiterChange("ACCEPT",2))).andExpect(status().isConflict());
 }
 String jobPreferenceBody(String query,String location,String workplace,long version)throws Exception{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(new com.meridian.jobs.domain.JobPreferences(query,location,workplace==null?null:com.meridian.jobs.domain.Workplace.valueOf(workplace),version));}
 @Test @org.springframework.transaction.annotation.Transactional
 void jobPreferencesArePrivateValidatedAndVersioned()throws Exception {
  String url="/api/v1/me/job-preferences";var owner=oidcLogin().idToken(t->t.subject("job-pref-owner"));var other=oidcLogin().idToken(t->t.subject("job-pref-other"));
  mvc.perform(get(url)).andExpect(status().isUnauthorized());
  mvc.perform(get(url).with(owner)).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.version").value(0));
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobPreferenceBody("Engineer","Test","REMOTE",0))).andExpect(status().isConflict());organizationMember("job-pref-owner");
  mvc.perform(put(url).with(owner).contentType("application/json").content(jobPreferenceBody("Engineer","Test","REMOTE",0))).andExpect(status().isForbidden());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobPreferenceBody("x".repeat(81),"Test","REMOTE",0))).andExpect(status().isBadRequest());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobPreferenceBody(" Engineer "," Test ","REMOTE",0))).andExpect(status().isOk()).andExpect(jsonPath("$.query").value("Engineer")).andExpect(jsonPath("$.version").value(1));
  mvc.perform(get(url).with(other)).andExpect(jsonPath("$.query").value("")).andExpect(jsonPath("$.version").value(0));
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobPreferenceBody("Overwrite","","ONSITE",0))).andExpect(status().isConflict());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobPreferenceBody("","",null,1))).andExpect(status().isOk()).andExpect(jsonPath("$.workplace").isEmpty()).andExpect(jsonPath("$.version").value(2));
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void preferredJobsMatchSavedCriteriaBeforePagingAndExcludeClosedJobs()throws Exception {
  var company=jobCompany("preferred-publisher");organizationMember("preferred-member");var publisher=oidcLogin().idToken(t->t.subject("preferred-publisher"));var member=oidcLogin().idToken(t->t.subject("preferred-member"));
  for(int i=0;i<22;i++){var id=java.util.UUID.randomUUID();mvc.perform(put("/api/v1/jobs/"+id).with(publisher).with(csrf()).contentType("application/json").content(jobBody(company,"Preferred%_ engineer "+i,"OPEN",0))).andExpect(status().isOk());if(i==21)mvc.perform(put("/api/v1/jobs/"+id).with(publisher).with(csrf()).contentType("application/json").content(jobBody(company,"Preferred%_ engineer "+i,"CLOSED",1))).andExpect(status().isOk());}
  mvc.perform(get("/api/v1/jobs?view=PREFERRED").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/me/job-preferences").with(member).with(csrf()).contentType("application/json").content(jobPreferenceBody("preferred%_","CITY","REMOTE",0))).andExpect(status().isOk());
  String body=mvc.perform(get("/api/v1/jobs").with(member).param("view","PREFERRED").param("organization",company.toString())).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
  mvc.perform(get("/api/v1/jobs").with(member).param("view","PREFERRED").param("organization",company.toString()).param("after",cursor)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/jobs?view=PREFERRED&workplace=HYBRID").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/jobs?view=PREFERRED&q=absent").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/jobs?view=PREFERRED").with(publisher)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/me/job-preferences").with(member).with(csrf()).contentType("application/json").content(jobPreferenceBody("","",null,1))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/jobs").with(member).param("view","PREFERRED").param("organization",company.toString())).andExpect(jsonPath("$.items.length()").value(20));
 }
 @Test void concurrentJobPreferenceWritersHaveOneWinner()throws Exception {
  organizationMember("job-preference-race");String url="/api/v1/me/job-preferences",initial=jobPreferenceBody("Initial","",null,0),updated=jobPreferenceBody("Changed","","REMOTE",1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(8)){
   for(String body:java.util.List.of(initial,updated)){var tasks=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();for(int i=0;i<8;i++)tasks.add(pool.submit(()->mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("job-preference-race"))).with(csrf()).contentType("application/json").content(body)).andReturn().getResponse().getStatus()));int successes=0;for(var task:tasks){int code=task.get();if(code==200)successes++;else org.junit.jupiter.api.Assertions.assertEquals(409,code);}org.junit.jupiter.api.Assertions.assertEquals(1,successes);}
  }
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void applicationHistoryIsCommittedChronologicalAndPartyScoped()throws Exception {
  var job=applicationJob("history-owner");String id=submitApplication(job,"history-member"),history="/api/v1/applications/"+id+"/history",statusPath="/api/v1/applications/"+id+"/status";
  var owner=oidcLogin().idToken(t->t.subject("history-owner"));var member=oidcLogin().idToken(t->t.subject("history-member"));var stranger=oidcLogin().idToken(t->t.subject("history-stranger"));
  mvc.perform(get(history)).andExpect(status().isUnauthorized());
  mvc.perform(get(history).with(stranger)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/applications/"+java.util.UUID.randomUUID()+"/history").with(owner)).andExpect(status().isNotFound());
  mvc.perform(get(history).with(member)).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].fromStatus").isEmpty()).andExpect(jsonPath("$.items[0].actorRole").value("APPLICANT"));
  submitApplication(job,"history-member");
  mvc.perform(put(statusPath).with(owner).with(csrf()).contentType("application/json").content("{\"status\":\"REVIEWING\",\"version\":1}")).andExpect(status().isOk());
  mvc.perform(put(statusPath).with(owner).with(csrf()).contentType("application/json").content("{\"status\":\"ACCEPTED\",\"version\":2}")).andExpect(status().isOk());
  mvc.perform(put(statusPath).with(member).with(csrf()).contentType("application/json").content("{\"status\":\"WITHDRAWN\",\"version\":3}")).andExpect(status().isConflict());
  for(var actor:java.util.List.of(owner,member))mvc.perform(get(history).with(actor)).andExpect(jsonPath("$.version").value(3)).andExpect(jsonPath("$.items.length()").value(3)).andExpect(jsonPath("$.items[0].version").value(1)).andExpect(jsonPath("$.items[1].fromStatus").value("SUBMITTED")).andExpect(jsonPath("$.items[1].toStatus").value("REVIEWING")).andExpect(jsonPath("$.items[2].version").value(3)).andExpect(jsonPath("$.items[2].actorRole").value("COMPANY_OWNER")).andExpect(jsonPath("$.items[2].toStatus").value("ACCEPTED")).andExpect(jsonPath("$.items[2].actor").doesNotExist()).andExpect(jsonPath("$.items[2].statement").doesNotExist());
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void applicationStatusFiltersComposeWithPaginationAndOwnership()throws Exception {
  var job=applicationJob("status-filter-owner");var owner=oidcLogin().idToken(t->t.subject("status-filter-owner"));String rejected=submitApplication(job,"status-filter-rejected");
  mvc.perform(put("/api/v1/applications/"+rejected+"/status").with(owner).with(csrf()).contentType("application/json").content("{\"status\":\"REJECTED\",\"version\":1}")).andExpect(status().isOk());
  for(int i=0;i<21;i++)submitApplication(job,"status-filter-"+i);
  String json=mvc.perform(get("/api/v1/applications").with(owner).param("view","RECEIVED").param("job",job.toString()).param("status","SUBMITTED")).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("nextCursor").asText();
  mvc.perform(get("/api/v1/applications").with(owner).param("view","RECEIVED").param("job",job.toString()).param("status","SUBMITTED").param("after",cursor)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/applications?view=RECEIVED&status=REJECTED").with(owner)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(rejected));
  var member=oidcLogin().idToken(t->t.subject("status-filter-rejected"));
  mvc.perform(get("/api/v1/applications?status=SUBMITTED").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/applications?status=REJECTED").with(member)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/applications?view=RECEIVED&status=REJECTED").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/applications?status=UNKNOWN").with(owner)).andExpect(status().isBadRequest());
 }
 java.util.UUID applicationJob(String owner)throws Exception {var company=jobCompany(owner);var id=java.util.UUID.randomUUID();mvc.perform(put("/api/v1/jobs/"+id).with(oidcLogin().idToken(t->t.subject(owner))).with(csrf()).contentType("application/json").content(jobBody(company,"Application role","OPEN",0))).andExpect(status().isOk());return id;}
 String submitApplication(java.util.UUID job,String subject)throws Exception {organizationMember(subject);String body=mvc.perform(put("/api/v1/jobs/"+job+"/application").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"statement\":\"My private application statement\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("id").asText();}
 @Test @org.springframework.transaction.annotation.Transactional
 void applicationsPreserveSnapshotsAndEnforcePrivacyAndRetries()throws Exception {
  var job=applicationJob("application-owner");organizationMember("application-member");var owner=oidcLogin().idToken(t->t.subject("application-owner"));var member=oidcLogin().idToken(t->t.subject("application-member"));var stranger=oidcLogin().idToken(t->t.subject("application-stranger"));String url="/api/v1/jobs/"+job+"/application";
  mvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
  mvc.perform(put(url).with(member).contentType("application/json").content("{\"statement\":\"Test\"}")).andExpect(status().isForbidden());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content("{\"statement\":\"Test\"}")).andExpect(status().isConflict());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content("{\"statement\":\" \"}")).andExpect(status().isBadRequest());
  database.update("update profile set name='Frozen Applicant',headline='Frozen headline' where subject='application-member'");String id=submitApplication(job,"application-member");
  org.junit.jupiter.api.Assertions.assertEquals(id,submitApplication(job,"application-member"));
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content("{\"statement\":\"Changed\"}")).andExpect(status().isConflict());
  database.update("update profile set name='New Name',headline='New headline' where subject='application-member'");
  mvc.perform(get("/api/v1/applications?view=RECEIVED").with(owner)).andExpect(jsonPath("$.items[0].applicantName").value("Frozen Applicant")).andExpect(jsonPath("$.items[0].applicantHeadline").value("Frozen headline")).andExpect(jsonPath("$.items[0].mine").value(false));
  for(String view:java.util.List.of("MINE","RECEIVED"))mvc.perform(get("/api/v1/applications?view="+view).with(stranger)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/applications/"+id+"/status").with(stranger).with(csrf()).contentType("application/json").content("{\"status\":\"REJECTED\",\"version\":1}")).andExpect(status().isNotFound());
  mvc.perform(put("/api/v1/applications/"+id+"/status").with(member).with(csrf()).contentType("application/json").content("{\"status\":\"ACCEPTED\",\"version\":1}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/applications/"+id+"/status").with(owner).with(csrf()).contentType("application/json").content("{\"status\":\"WITHDRAWN\",\"version\":1}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/applications/"+id+"/status").with(member).with(csrf()).contentType("application/json").content("{\"status\":\"WITHDRAWN\",\"version\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WITHDRAWN"));
  org.junit.jupiter.api.Assertions.assertEquals(id,submitApplication(job,"application-member"));
  mvc.perform(put("/api/v1/applications/"+id+"/status").with(owner).with(csrf()).contentType("application/json").content("{\"status\":\"REVIEWING\",\"version\":2}")).andExpect(status().isConflict());
  org.junit.jupiter.api.Assertions.assertEquals(2,database.queryForObject("select count(*) from application_audit where application_id=?",Integer.class,java.util.UUID.fromString(id)));
  String rejected=submitApplication(job,"application-rejected");
  mvc.perform(put("/api/v1/applications/"+rejected+"/status").with(owner).with(csrf()).contentType("application/json").content("{\"status\":\"REJECTED\",\"version\":1}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
  mvc.perform(put("/api/v1/applications/"+rejected+"/status").with(oidcLogin().idToken(t->t.subject("application-rejected"))).with(csrf()).contentType("application/json").content("{\"status\":\"WITHDRAWN\",\"version\":2}")).andExpect(status().isConflict());
 }
 @Test void applicationRetriesAndConcurrentReviewWithdrawalAreAtomic()throws Exception {
  var job=applicationJob("application-race-owner");organizationMember("application-race-member");String url="/api/v1/jobs/"+job+"/application";
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(8)){
   var tasks=new java.util.ArrayList<java.util.concurrent.Future<String>>();for(int i=0;i<8;i++)tasks.add(pool.submit(()->mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("application-race-member"))).with(csrf()).contentType("application/json").content("{\"statement\":\"Concurrent statement\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
   var ids=new java.util.HashSet<String>();for(var task:tasks)ids.add(new com.fasterxml.jackson.databind.ObjectMapper().readTree(task.get()).get("id").asText());org.junit.jupiter.api.Assertions.assertEquals(1,ids.size());String id=ids.iterator().next(),path="/api/v1/applications/"+id+"/status";
   mvc.perform(put(path).with(oidcLogin().idToken(t->t.subject("application-race-owner"))).with(csrf()).contentType("application/json").content("{\"status\":\"ACCEPTED\",\"version\":1}")).andExpect(status().isConflict());
   mvc.perform(put(path).with(oidcLogin().idToken(t->t.subject("application-race-owner"))).with(csrf()).contentType("application/json").content("{\"status\":\"REVIEWING\",\"version\":1}")).andExpect(status().isOk());
   var gate=new java.util.concurrent.CountDownLatch(1);var accept=pool.submit(()->{gate.await();return mvc.perform(put(path).with(oidcLogin().idToken(t->t.subject("application-race-owner"))).with(csrf()).contentType("application/json").content("{\"status\":\"ACCEPTED\",\"version\":2}")).andReturn().getResponse().getStatus();});
   var withdraw=pool.submit(()->{gate.await();return mvc.perform(put(path).with(oidcLogin().idToken(t->t.subject("application-race-member"))).with(csrf()).contentType("application/json").content("{\"status\":\"WITHDRAWN\",\"version\":2}")).andReturn().getResponse().getStatus();});gate.countDown();var codes=new java.util.ArrayList<>(java.util.List.of(accept.get(),withdraw.get()));java.util.Collections.sort(codes);org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(200,409),codes);
   org.junit.jupiter.api.Assertions.assertEquals(3,database.queryForObject("select count(*) from application_audit where application_id=?",Integer.class,java.util.UUID.fromString(id)));
  }
 }
 @Test void jobClosingSerializesWithApplicationSubmission()throws Exception {
  var job=applicationJob("application-close-owner");organizationMember("application-close-member");var company=database.queryForObject("select organization_id from job where id=?",java.util.UUID.class,job);String closeBody=jobBody(company,"Application role","CLOSED",1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var gate=new java.util.concurrent.CountDownLatch(1);var closing=pool.submit(()->{gate.await();return mvc.perform(put("/api/v1/jobs/"+job).with(oidcLogin().idToken(t->t.subject("application-close-owner"))).with(csrf()).contentType("application/json").content(closeBody)).andReturn().getResponse().getStatus();});
   var applying=pool.submit(()->{gate.await();return mvc.perform(put("/api/v1/jobs/"+job+"/application").with(oidcLogin().idToken(t->t.subject("application-close-member"))).with(csrf()).contentType("application/json").content("{\"statement\":\"Closure race\"}")).andReturn().getResponse().getStatus();});gate.countDown();org.junit.jupiter.api.Assertions.assertEquals(200,closing.get());int result=applying.get();org.junit.jupiter.api.Assertions.assertTrue(result==200||result==409);org.junit.jupiter.api.Assertions.assertEquals(result==200?1:0,database.queryForObject("select count(*) from job_application where job_id=?",Integer.class,job));
  }
  organizationMember("application-late-member");mvc.perform(put("/api/v1/jobs/"+job+"/application").with(oidcLogin().idToken(t->t.subject("application-late-member"))).with(csrf()).contentType("application/json").content("{\"statement\":\"Too late\"}")).andExpect(status().isConflict());
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void applicationPaginationKeepsOwnerAndApplicantBoundaries()throws Exception {
  var job=applicationJob("application-page-owner");for(int i=0;i<21;i++)submitApplication(job,"application-page-"+i);
  var owner=oidcLogin().idToken(t->t.subject("application-page-owner"));String body=mvc.perform(get("/api/v1/applications?view=RECEIVED&job="+job).with(owner)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
  mvc.perform(get("/api/v1/applications?view=RECEIVED&job="+job+"&after="+cursor).with(owner)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/applications?job="+job).with(oidcLogin().idToken(t->t.subject("application-page-0")))).andExpect(jsonPath("$.items.length()").value(1));
 }
 java.util.UUID jobCompany(String subject)throws Exception {
  organizationMember(subject);var id=java.util.UUID.randomUUID();mvc.perform(put("/api/v1/organizations/"+id).with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content(organizationBody("Fictional jobs company",0))).andExpect(status().isOk());return id;
 }
 String jobBody(java.util.UUID organization,String title,String state,long version)throws Exception{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(new com.meridian.jobs.domain.Jobs.Write(organization,title,"Test city","Fictional role description",com.meridian.jobs.domain.Workplace.REMOTE,com.meridian.jobs.domain.Jobs.Status.valueOf(state),version));}
 @Test @org.springframework.transaction.annotation.Transactional
 void jobsEnforceOwnershipLifecycleAndPrivateSavedState()throws Exception {
  var company=jobCompany("job-owner");organizationMember("job-member");organizationMember("job-other");
  var owner=oidcLogin().idToken(t->t.subject("job-owner"));var member=oidcLogin().idToken(t->t.subject("job-member"));var other=oidcLogin().idToken(t->t.subject("job-other"));
  String url="/api/v1/jobs/"+java.util.UUID.randomUUID(),body=jobBody(company,"Engineer","OPEN",0);
  mvc.perform(get(url)).andExpect(status().isUnauthorized());
  mvc.perform(put(url).with(owner).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobBody(company,"Engineer","CLOSED",0))).andExpect(status().isBadRequest());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobBody(company," ","OPEN",0))).andExpect(status().isBadRequest());
  for(int i=0;i<2;i++)mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(put(url+"/saved").with(member)).andExpect(status().isForbidden());
  for(int i=0;i<2;i++)mvc.perform(put(url+"/saved").with(member).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(url).with(member)).andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.owned").value(false));
  mvc.perform(delete(url+"/saved").with(other).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/jobs?view=SAVED").with(other)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(url).with(member).with(csrf()).contentType("application/json").content(jobBody(company,"Stolen","CLOSED",1))).andExpect(status().isForbidden());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobBody(company,"Engineer","CLOSED",1))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/jobs").param("organization",company.toString()).with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/jobs?view=SAVED").with(member)).andExpect(jsonPath("$.items[0].status").value("CLOSED"));
  mvc.perform(get("/api/v1/jobs?view=MANAGED").with(member)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobBody(company,"Engineer","OPEN",1))).andExpect(status().isConflict());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobBody(company,"Engineer","OPEN",2))).andExpect(status().isOk());
  var second=jobCompany("job-owner");mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(jobBody(second,"Moved","OPEN",3))).andExpect(status().isConflict());
  for(int i=0;i<2;i++)mvc.perform(delete(url+"/saved").with(member).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(url).with(member)).andExpect(jsonPath("$.saved").value(false));
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void jobFiltersComposeBeforePagination()throws Exception {
  var company=jobCompany("jobs-search");var owner=oidcLogin().idToken(t->t.subject("jobs-search"));
  for(int i=0;i<21;i++)mvc.perform(put("/api/v1/jobs/"+java.util.UUID.randomUUID()).with(owner).with(csrf()).contentType("application/json").content(jobBody(company,"Literal%_ "+i,"OPEN",0))).andExpect(status().isOk());
  String json=mvc.perform(get("/api/v1/jobs").with(owner).param("q","  LITERAL%_ ").param("location","CITY").param("workplace","REMOTE").param("organization",company.toString())).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("nextCursor").asText();
  mvc.perform(get("/api/v1/jobs").with(owner).param("q","literal%_").param("organization",company.toString()).param("after",cursor)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/jobs").with(owner).param("organization",company.toString()).param("workplace","HYBRID")).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/jobs").with(owner).param("q","' OR true --")).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/jobs").with(owner).param("q","x".repeat(81))).andExpect(status().isBadRequest());
 }
 @Test void concurrentJobRetriesAndStateChangesPreserveVersions()throws Exception {
  var company=jobCompany("jobs-race");String url="/api/v1/jobs/"+java.util.UUID.randomUUID(),create=jobBody(company,"Concurrent role","OPEN",0),close=jobBody(company,"Concurrent role","CLOSED",1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(8)){
   var tasks=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
   for(int i=0;i<8;i++)tasks.add(pool.submit(()->mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("jobs-race"))).with(csrf()).contentType("application/json").content(create)).andReturn().getResponse().getStatus()));
   for(var task:tasks)org.junit.jupiter.api.Assertions.assertEquals(200,task.get());tasks.clear();
   for(int i=0;i<8;i++)tasks.add(pool.submit(()->mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("jobs-race"))).with(csrf()).contentType("application/json").content(close)).andReturn().getResponse().getStatus()));
   int successes=0;for(var task:tasks){int status=task.get();if(status==200)successes++;else org.junit.jupiter.api.Assertions.assertEquals(409,status);}org.junit.jupiter.api.Assertions.assertEquals(1,successes);
  }
 }
 String organizationBody(String name,long version)throws Exception{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(new com.meridian.organizations.domain.Organizations.Write(name,"Developer tools","Test city","Fictional company overview",version));}
 void organizationMember(String subject)throws Exception{mvc.perform(get("/api/v1/me").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isOk());}
 @Test @org.springframework.transaction.annotation.Transactional
 void companyOwnershipCreationRetryValidationAndVersions()throws Exception {
  String id=java.util.UUID.randomUUID().toString(),url="/api/v1/organizations/"+id;
  var owner=oidcLogin().idToken(t->t.subject("company-owner"));var other=oidcLogin().idToken(t->t.subject("company-other"));
  mvc.perform(get(url)).andExpect(status().isUnauthorized());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Acme",0))).andExpect(status().isConflict());
  organizationMember("company-owner");organizationMember("company-other");
  mvc.perform(put(url).with(owner).contentType("application/json").content(organizationBody("Acme",0))).andExpect(status().isForbidden());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("  ",0))).andExpect(status().isBadRequest());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("x".repeat(161),0))).andExpect(status().isBadRequest());
  for(int i=0;i<2;i++)mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Acme",0))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.owned").value(true));
  mvc.perform(put(url).with(other).with(csrf()).contentType("application/json").content(organizationBody("Acme",0))).andExpect(status().isConflict());
  mvc.perform(put(url).with(other).with(csrf()).contentType("application/json").content(organizationBody("Stolen",1))).andExpect(status().isForbidden());
  mvc.perform(get(url).with(other)).andExpect(jsonPath("$.name").value("Acme")).andExpect(jsonPath("$.owned").value(false)).andExpect(jsonPath("$.owner_subject").doesNotExist());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Updated",1))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Stale",1))).andExpect(status().isConflict());
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Acme",0))).andExpect(status().isConflict());
  mvc.perform(get(url).with(owner)).andExpect(jsonPath("$.name").value("Updated"));
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void companyFollowsArePrivateIdempotentAndCallerScoped()throws Exception {
  String id=java.util.UUID.randomUUID().toString(),url="/api/v1/organizations/"+id;
  for(String subject:java.util.List.of("follow-company-owner","follow-company-a","follow-company-b"))organizationMember(subject);
  var owner=oidcLogin().idToken(t->t.subject("follow-company-owner"));var a=oidcLogin().idToken(t->t.subject("follow-company-a"));var b=oidcLogin().idToken(t->t.subject("follow-company-b"));
  mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Fictional Labs",0))).andExpect(status().isOk());
  mvc.perform(put(url+"/follow").with(a)).andExpect(status().isForbidden());
  for(int i=0;i<2;i++)mvc.perform(put(url+"/follow").with(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(url).with(a)).andExpect(jsonPath("$.following").value(true)).andExpect(jsonPath("$.followerCount").value(1));
  mvc.perform(get(url).with(b)).andExpect(jsonPath("$.following").value(false));
  mvc.perform(delete(url+"/follow").with(b).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/organizations?view=FOLLOWING").with(a)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/organizations?view=FOLLOWING").with(b)).andExpect(jsonPath("$.items.length()").value(0));
  for(int i=0;i<2;i++)mvc.perform(delete(url+"/follow").with(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(url).with(owner)).andExpect(jsonPath("$.followerCount").value(0));
  mvc.perform(put("/api/v1/organizations/"+java.util.UUID.randomUUID()+"/follow").with(a).with(csrf())).andExpect(status().isNotFound());
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void companyDirectoryFiltersAndPaginatesLiteralSearch()throws Exception {
  organizationMember("directory-company");organizationMember("directory-other");var owner=oidcLogin().idToken(t->t.subject("directory-company"));
  for(int i=0;i<21;i++)mvc.perform(put("/api/v1/organizations/"+java.util.UUID.randomUUID()).with(owner).with(csrf()).contentType("application/json").content(organizationBody("Literal%_ "+i,0))).andExpect(status().isOk());
  String json=mvc.perform(get("/api/v1/organizations").param("q","  literal%_  ").param("view","MANAGED").with(owner)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
  var tree=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
  mvc.perform(get("/api/v1/organizations").param("q","literal%_").param("after",tree.get("nextCursor").asText()).with(owner)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/organizations").param("q","literal%_").param("view","MANAGED").with(oidcLogin().idToken(t->t.subject("directory-other")))).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/organizations").param("q","' OR true --").with(owner)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/organizations").param("q","x".repeat(81)).with(owner)).andExpect(status().isBadRequest());
 }
 @Test void concurrentCompanyCreationAndUpdatesHaveSingleWinners()throws Exception {
  organizationMember("company-race");String url="/api/v1/organizations/"+java.util.UUID.randomUUID(),create=organizationBody("Concurrent Labs",0),update=organizationBody("New Labs",1);
  try(var pool=java.util.concurrent.Executors.newFixedThreadPool(8)){
   var start=new java.util.concurrent.CountDownLatch(1);var tasks=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
   for(int i=0;i<8;i++)tasks.add(pool.submit(()->{start.await();return mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("company-race"))).with(csrf()).contentType("application/json").content(create)).andReturn().getResponse().getStatus();}));
   start.countDown();for(var task:tasks)org.junit.jupiter.api.Assertions.assertEquals(200,task.get());
   tasks.clear();for(int i=0;i<8;i++)tasks.add(pool.submit(()->mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject("company-race"))).with(csrf()).contentType("application/json").content(update)).andReturn().getResponse().getStatus()));
   int successes=0;for(var task:tasks){int code=task.get();if(code==200)successes++;else org.junit.jupiter.api.Assertions.assertEquals(409,code);}org.junit.jupiter.api.Assertions.assertEquals(1,successes);
  }
 }
 @Autowired org.springframework.jdbc.core.JdbcTemplate database;
 @Test void jobDismissalPersistsPerMemberWithoutRemovingSavesOrJobs()throws Exception{
  var json=new com.fasterxml.jackson.databind.ObjectMapper();
  var job=applicationJob("dismiss-owner");mvc.perform(get("/api/v1/me").with(oidcLogin().idToken(t->t.subject("dismiss-member")))).andExpect(status().isOk());
  var member=oidcLogin().idToken(t->t.subject("dismiss-member"));var owner=oidcLogin().idToken(t->t.subject("dismiss-owner"));String endpoint="/api/v1/jobs/"+job+"/dismissed";
  mvc.perform(put(endpoint).with(member)).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/jobs/"+job+"/saved").with(member).with(csrf())).andExpect(status().isNoContent());
  for(int retry=0;retry<2;retry++)mvc.perform(put(endpoint).with(member).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/jobs?view=DISMISSED").with(member)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(job.toString()));
  mvc.perform(get("/api/v1/jobs?view=DISMISSED").with(owner)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/jobs?view=SAVED").with(member)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/jobs/"+job).with(member)).andExpect(status().isOk());
  mvc.perform(delete(endpoint).with(owner).with(csrf())).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from dismissed_job where job_id=?",Integer.class,job));
  var hidden=json.readTree(mvc.perform(get("/api/v1/jobs").with(member)).andReturn().getResponse().getContentAsString());
  for(var row:hidden.get("items"))org.junit.jupiter.api.Assertions.assertNotEquals(job.toString(),row.get("id").asText());
  for(int retry=0;retry<2;retry++)mvc.perform(delete(endpoint).with(member).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/jobs?view=DISMISSED").with(member)).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test void notificationReadLifecycleIsOwnedIdempotentAndBounded()throws Exception{
  var json=new com.fasterxml.jackson.databind.ObjectMapper();
  var fixture=chat("notification-life");var owner=oidcLogin().idToken(t->t.subject("notification-life-a"));var other=oidcLogin().idToken(t->t.subject("notification-life-b"));
  var publisher=org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);var projector=projectionFixture(publisher);
  var id=java.util.UUID.randomUUID();var event=new ConnectionAccepted(id,1,"ConnectionAccepted",java.util.UUID.fromString(fixture.connection()));projector.project(event);
  var page=json.readTree(mvc.perform(get("/api/v1/notifications").with(owner)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  String through=page.get("readThrough").asText();String read="/api/v1/notifications/"+id+"/read";String dismiss="/api/v1/notifications/"+id;
  mvc.perform(delete(read)).andExpect(status().isForbidden());
  mvc.perform(delete(read).with(other).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(delete(dismiss).with(other).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(delete(dismiss).with(owner)).andExpect(status().isForbidden());
  mvc.perform(put(read).with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(delete(read).with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(delete(read).with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notifications").with(owner)).andExpect(jsonPath("$.unreadCount").value(1));
  var later=java.util.UUID.randomUUID();database.update("insert into notification(id,connection_id,recipient,actor) values(?,?,?,?)",later,java.util.UUID.fromString(fixture.connection()),"notification-life-a","notification-life-b");
  mvc.perform(put("/api/v1/notifications/read").with(owner).with(csrf()).contentType("application/json").content("{\"through\":\"9999999999999999999\"}")).andExpect(status().isBadRequest());
  for(int retry=0;retry<2;retry++)mvc.perform(put("/api/v1/notifications/read").with(owner).with(csrf()).contentType("application/json").content(json.writeValueAsString(java.util.Map.of("through",through)))).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notifications").with(owner)).andExpect(jsonPath("$.unreadCount").value(1)).andExpect(jsonPath("$.items[0].read").value(false));
  mvc.perform(delete(dismiss).with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(delete(dismiss).with(owner).with(csrf())).andExpect(status().isNoContent());
  projector.project(event);
  mvc.perform(get("/api/v1/notifications").with(owner)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(delete(read).with(owner).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(put(read).with(owner).with(csrf())).andExpect(status().isNotFound());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from notification where id=?",Integer.class,id));
 }
 @Test @org.springframework.transaction.annotation.Transactional
 void notificationSignalsOnlyFollowNewVisibleProjections()throws Exception {
  var fixture=chat("notification-live");
  var events=org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class);
  var projector=projectionFixture(events);
  var event=new ConnectionAccepted(java.util.UUID.randomUUID(),1,"ConnectionAccepted",java.util.UUID.fromString(fixture.connection()));
  projector.project(event);projector.project(event);
  org.mockito.Mockito.verify(events,org.mockito.Mockito.times(1)).publishEvent(new RealtimeChanged(java.util.Set.of("notification-live-a")));
  org.mockito.Mockito.verifyNoMoreInteractions(events);
  database.update("insert into notification_preference(subject,connections) values(?,false)","notification-live-a");
  projector.project(new ConnectionAccepted(java.util.UUID.randomUUID(),1,"ConnectionAccepted",java.util.UUID.fromString(fixture.connection())));
  org.mockito.Mockito.verifyNoMoreInteractions(events);
  database.update("update notification_preference set connections=true where subject=?","notification-live-a");
  database.update("insert into member_block(blocker,blocked) values(?,?)","notification-live-b","notification-live-a");
  projector.project(new ConnectionAccepted(java.util.UUID.randomUUID(),1,"ConnectionAccepted",java.util.UUID.fromString(fixture.connection())));
  org.mockito.Mockito.verifyNoMoreInteractions(events);
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from notification where recipient=?",Integer.class,"notification-live-a"));
 }
 @Autowired RealtimeHandler realtime;
 @Autowired org.springframework.context.ApplicationEventPublisher publisher;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
 @Test void realtimeSignalsRequireCommittedTransactions()throws Exception {
  var fixture=new RealtimeHandlerTest().client("transaction-signal");
  database.update("insert into spring_session(primary_id,session_id,creation_time,last_access_time,max_inactive_interval,expiry_time,principal_name) values (?,?,?,?,?,?,?)",java.util.UUID.randomUUID().toString(),fixture.session().getId(),System.currentTimeMillis(),System.currentTimeMillis(),1800,System.currentTimeMillis()+1800000,"transaction-signal");
  realtime.afterConnectionEstablished(fixture.socket());
  org.mockito.Mockito.verify(fixture.socket(),org.mockito.Mockito.timeout(2000)).sendMessage(org.mockito.ArgumentMatchers.any());
  var transaction=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
  try {
   publisher.publishEvent(new RealtimeChanged(java.util.Set.of("transaction-signal")));
   org.mockito.Mockito.verify(fixture.socket(),org.mockito.Mockito.after(100).times(1)).sendMessage(org.mockito.ArgumentMatchers.any());
   transaction.executeWithoutResult(status->{publisher.publishEvent(new RealtimeChanged(java.util.Set.of("transaction-signal")));status.setRollbackOnly();});
   org.mockito.Mockito.verify(fixture.socket(),org.mockito.Mockito.after(100).times(1)).sendMessage(org.mockito.ArgumentMatchers.any());
   transaction.executeWithoutResult(status->publisher.publishEvent(new RealtimeChanged(java.util.Set.of("transaction-signal"))));
   org.mockito.Mockito.verify(fixture.socket(),org.mockito.Mockito.timeout(2000).times(2)).sendMessage(org.mockito.ArgumentMatchers.any());
  } finally {realtime.afterConnectionClosed(fixture.socket(),org.springframework.web.socket.CloseStatus.NORMAL);}
 }
 @Test void anonymousIsDenied()throws Exception{mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized()).andExpect(cookie().doesNotExist("JSESSIONID"));}
 @Test void persistenceConcurrencyAndIsolation()throws Exception{
 var alice=oidcLogin().idToken(t->t.subject("alice"));
 mvc.perform(get("/api/v1/me").with(alice)).andExpect(status().isOk());
 String body="{\"name\":\"Alice Test\",\"headline\":\"Engineer\",\"location\":\"Test city\",\"about\":\"Hello\",\"version\":0}";
 mvc.perform(put("/api/v1/me").with(alice).contentType("application/json").content(body)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/me").with(alice).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
 mvc.perform(get("/api/v1/me").with(alice)).andExpect(jsonPath("$.headline").value("Engineer"));
 mvc.perform(put("/api/v1/me").with(alice).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 mvc.perform(get("/api/v1/me").with(oidcLogin().idToken(t->t.subject("bob")))).andExpect(jsonPath("$.headline").value(""));
 }
 @Test void logoutRequiresCsrf()throws Exception{
 mvc.perform(post("/api/v1/logout").with(oidcLogin())).andExpect(status().isForbidden());
 }
 @Test void nullFieldsRejected()throws Exception{
 mvc.perform(put("/api/v1/me").with(oidcLogin()).with(csrf()).contentType("application/json").content("{\"name\":\"Test\",\"version\":0}")).andExpect(status().isBadRequest());
 }
 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"experience","education","certifications","projects"})
 void sectionOwnershipVersionAndDates(String section)throws Exception{
 var owner=oidcLogin().idToken(t->t.subject(section+"-owner"));
 var stranger=oidcLogin().idToken(t->t.subject("stranger"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 String id=java.util.UUID.randomUUID().toString();
 String body="""
 {"id":"%s","title":"Engineer","organization":"Fictional Labs","startDate":"2020-01-01","endDate":null,"description":"Work","version":0}
 """.formatted(id);
 if(section.equals("projects")) body=body.replace("\"description\":", "\"associatedSkills\":\"Java, SQL\",\"description\":");
 if(section.equals("education")) body=body.replace("title","degree").replace("organization","institution").replace("\"description\":", "\"fieldOfStudy\":\"Computing\",\"description\":");
 if(section.equals("certifications")) body=body.replace("title","name").replace("organization","issuer").replace("startDate","issueDate").replace("endDate","expiryDate").replace("\"description\":\"Work\"", "\"credentialId\":\"TEST-ID\",\"credentialUrl\":\"https://example.test/credential\"");
 mvc.perform(put("/api/v1/me/"+section).with(stranger).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/"+section).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
 mvc.perform(put("/api/v1/me/"+section).with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 mvc.perform(get("/api/v1/me/"+section).with(stranger)).andExpect(jsonPath("$.length()").value(0));
 String updated=body.replace("\"version\":0","\"version\":1");
 mvc.perform(put("/api/v1/me/"+section).with(stranger).with(csrf()).contentType("application/json").content(updated)).andExpect(status().isConflict());
 mvc.perform(delete("/api/v1/me/"+section+"/"+id+"?version=1").with(stranger).with(csrf())).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/"+section).with(owner).with(csrf()).contentType("application/json").content(updated.replace("null","\"2019-01-01\""))).andExpect(status().isBadRequest());
 mvc.perform(put("/api/v1/me/"+section).with(owner).with(csrf()).contentType("application/json").content(updated)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
 mvc.perform(delete("/api/v1/me/"+section+"/"+id+"?version=1").with(owner).with(csrf())).andExpect(status().isConflict());
 mvc.perform(delete("/api/v1/me/"+section+"/"+id+"?version=2").with(owner).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/me/"+section).with(owner)).andExpect(jsonPath("$.length()").value(0));
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"javascript:alert(1)","data:text/html,test","http://example.test","https://user:secret@example.test","/relative","https://"})
 void certificationRejectsUnsafeUrl(String url)throws Exception{
 String body="""
 {"id":"%s","name":"Certificate","issuer":"Test issuer","issueDate":"2020-01-01","expiryDate":null,"credentialId":"TEST","credentialUrl":"%s","version":0}
 """.formatted(java.util.UUID.randomUUID(),url);
 mvc.perform(put("/api/v1/me/certifications").with(oidcLogin()).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
 }

 @Test void skillsNormalizeNamesAndEnforceOwnership()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("skill-owner"));
 var other=oidcLogin().idToken(t->t.subject("skill-other"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 String id=java.util.UUID.randomUUID().toString();
 String body="""
 {"id":"%s","name":"  Java   Programming  ","version":0}
 """.formatted(id);
 mvc.perform(put("/api/v1/me/skills").with(owner).contentType("application/json").content(body)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/me/skills").with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Java Programming"));
 mvc.perform(put("/api/v1/me/skills").with(owner).with(csrf()).contentType("application/json").content(body.replace(id,java.util.UUID.randomUUID().toString()).replace("Java","java"))).andExpect(status().isConflict());
 mvc.perform(get("/api/v1/me/skills").with(other)).andExpect(jsonPath("$.length()").value(0));
 String update=body.replace("\"version\":0","\"version\":1").replace("Java","Python");
 mvc.perform(put("/api/v1/me/skills").with(other).with(csrf()).contentType("application/json").content(update)).andExpect(status().isConflict());
 mvc.perform(delete("/api/v1/me/skills/"+id+"?version=1").with(other).with(csrf())).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/skills").with(owner).with(csrf()).contentType("application/json").content(update)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
 mvc.perform(delete("/api/v1/me/skills/"+id+"?version=1").with(owner).with(csrf())).andExpect(status().isConflict());
 mvc.perform(delete("/api/v1/me/skills/"+id+"?version=2").with(owner).with(csrf())).andExpect(status().isNoContent());
 }
 @Test void languagesNormalizeNamesAndEnforceOwnership()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("language-owner"));
 var other=oidcLogin().idToken(t->t.subject("language-other"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 String id=java.util.UUID.randomUUID().toString();
 String body="""
 {"id":"%s","name":"  Java   Programming  ","proficiency":"PROFESSIONAL_WORKING","version":0}
 """.formatted(id);
 mvc.perform(put("/api/v1/me/languages").with(owner).contentType("application/json").content(body)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/me/languages").with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Java Programming"));
 mvc.perform(put("/api/v1/me/languages").with(owner).with(csrf()).contentType("application/json").content(body.replace(id,java.util.UUID.randomUUID().toString()).replace("Java","java"))).andExpect(status().isConflict());
 mvc.perform(get("/api/v1/me/languages").with(other)).andExpect(jsonPath("$.length()").value(0));
 String update=body.replace("\"version\":0","\"version\":1").replace("Java","Python");
 mvc.perform(put("/api/v1/me/languages").with(other).with(csrf()).contentType("application/json").content(update)).andExpect(status().isConflict());
 mvc.perform(delete("/api/v1/me/languages/"+id+"?version=1").with(other).with(csrf())).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/languages").with(owner).with(csrf()).contentType("application/json").content(update)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
 mvc.perform(delete("/api/v1/me/languages/"+id+"?version=1").with(owner).with(csrf())).andExpect(status().isConflict());
 mvc.perform(delete("/api/v1/me/languages/"+id+"?version=2").with(owner).with(csrf())).andExpect(status().isNoContent());
 }
 @Test void concurrentSkillDuplicatesHaveOneWinner()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("parallel-skills"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
 var gate=new java.util.concurrent.CountDownLatch(1);
 var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
 for(int i=0;i<8;i++)futures.add(executor.submit(()->{
 gate.await();
 String body="{\"id\":\"%s\",\"name\":\"Java\",\"version\":0}".formatted(java.util.UUID.randomUUID());
 return mvc.perform(put("/api/v1/me/skills").with(oidcLogin().idToken(t->t.subject("parallel-skills"))).with(csrf()).contentType("application/json").content(body)).andReturn().getResponse().getStatus();
 }));
 gate.countDown();int winners=0;
 for(var future:futures){int status=future.get(15,java.util.concurrent.TimeUnit.SECONDS);org.junit.jupiter.api.Assertions.assertTrue(status==200||status==409);if(status==200)winners++;}
 org.junit.jupiter.api.Assertions.assertEquals(1,winners);
 mvc.perform(get("/api/v1/me/skills").with(owner)).andExpect(jsonPath("$.length()").value(1));
 }
 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"null","\"EXPERT\""})
 void languageRejectsInvalidProficiency(String proficiency)throws Exception{
 String body="""
 {"id":"%s","name":"English","proficiency":%s,"version":0}
 """.formatted(java.util.UUID.randomUUID(),proficiency);
 mvc.perform(put("/api/v1/me/languages").with(oidcLogin()).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
 }

 @Test void preferencesPersistAndRejectStaleCrossAccountWrites()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("preferences-owner"));
 var other=oidcLogin().idToken(t->t.subject("preferences-other"));
 mvc.perform(get("/api/v1/me/preferences")).andExpect(status().isUnauthorized());
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 mvc.perform(get("/api/v1/me/preferences").with(owner)).andExpect(jsonPath("$.theme").value("system"));
 String body="""
 {"theme":"dark","version":0}
 """;
 mvc.perform(put("/api/v1/me/preferences").with(owner).contentType("application/json").content(body)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/me/preferences").with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
 mvc.perform(get("/api/v1/me/preferences").with(owner)).andExpect(jsonPath("$.theme").value("dark"));
 mvc.perform(put("/api/v1/me/preferences").with(owner).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/preferences").with(owner).with(csrf()).contentType("application/json").content(body.replace("dark","invalid"))).andExpect(status().isBadRequest());
 mvc.perform(get("/api/v1/me").with(other)).andExpect(status().isOk());
 mvc.perform(get("/api/v1/me/preferences").with(other)).andExpect(jsonPath("$.theme").value("system"));
 }

 @Autowired org.springframework.session.SessionRepository sessionRepository;
 String persistedSession(String subject){
  var session=sessionRepository.createSession();session.setAttribute(org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,subject);sessionRepository.save(session);return session.getId();
 }
 @Test void absoluteExpiryRemovesActiveSessionFromListingAndSocketAuthorization()throws Exception{
  String id=persistedSession("absolute-expired");
  database.update("update spring_session set creation_time=?,last_access_time=?,expiry_time=? where session_id=?",System.currentTimeMillis()-java.time.Duration.ofHours(13).toMillis(),System.currentTimeMillis(),System.currentTimeMillis()+600000,id);
  mvc.perform(get("/api/v1/me/sessions").with(oidcLogin().idToken(t->t.subject("absolute-expired")))).andExpect(jsonPath("$.length()").value(0));
  org.junit.jupiter.api.Assertions.assertFalse(sessionAccess.active(id,"absolute-expired"));
 }
 @Test void sessionListingRevocationAndAuditAreOwnerScoped()throws Exception{
  String first=persistedSession("session-owner"),second=persistedSession("session-owner"),foreign=persistedSession("session-other");
  String id=database.queryForObject("select primary_id from spring_session where session_id=?",String.class,first);
  String otherId=database.queryForObject("select primary_id from spring_session where session_id=?",String.class,foreign);
  var owner=oidcLogin().idToken(t->t.subject("session-owner"));
  mvc.perform(get("/api/v1/me/sessions")).andExpect(status().isUnauthorized());
  String listing=mvc.perform(get("/api/v1/me/sessions").with(owner)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.length()").value(2)).andReturn().getResponse().getContentAsString();
  org.junit.jupiter.api.Assertions.assertFalse(listing.contains(first));org.junit.jupiter.api.Assertions.assertFalse(listing.contains(second));org.junit.jupiter.api.Assertions.assertFalse(listing.contains(otherId));
  mvc.perform(delete("/api/v1/me/sessions/"+id).with(owner)).andExpect(status().isForbidden());
  mvc.perform(delete("/api/v1/me/sessions/"+otherId).with(owner).with(csrf())).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertNotNull(sessionRepository.findById(foreign));
  var stale=sessionRepository.findById(first);
  mvc.perform(delete("/api/v1/me/sessions/"+id).with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(delete("/api/v1/me/sessions/"+id).with(owner).with(csrf())).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertNull(sessionRepository.findById(first));
  stale.setLastAccessedTime(java.time.Instant.now());sessionRepository.save(stale);
  org.junit.jupiter.api.Assertions.assertNull(sessionRepository.findById(first));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from session_revocation_audit where subject='session-owner'",Integer.class));
  mvc.perform(delete("/api/v1/me/sessions").with(owner).with(csrf())).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertNull(sessionRepository.findById(second));org.junit.jupiter.api.Assertions.assertNotNull(sessionRepository.findById(foreign));
  org.junit.jupiter.api.Assertions.assertEquals(2,database.queryForObject("select count(*) from session_revocation_audit where subject='session-owner'",Integer.class));
 }
 @Test void expiredSessionsAreExcludedAndRejectedByRealtime()throws Exception{
  String id=persistedSession("session-expired");database.update("update spring_session set expiry_time=?,last_access_time=? where session_id=?",System.currentTimeMillis()-1000,System.currentTimeMillis()-3600000,id);
  mvc.perform(get("/api/v1/me/sessions").with(oidcLogin().idToken(t->t.subject("session-expired")))).andExpect(jsonPath("$.length()").value(0));
  org.junit.jupiter.api.Assertions.assertFalse(sessionAccess.active(id,"session-expired"));
  org.junit.jupiter.api.Assertions.assertNull(sessionRepository.findById(id));
 }

 @Test void profilePhotosPersistValidateAndRespectVisibility()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("photo-owner"));var other=oidcLogin().idToken(t->t.subject("photo-other"));
 mvc.perform(get("/api/v1/me").with(owner));
 String body=mvc.perform(get("/api/v1/me/visibility").with(owner)).andReturn().getResponse().getContentAsString();
 String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("publicId").asText();
 var image=new java.awt.image.BufferedImage(800,600,java.awt.image.BufferedImage.TYPE_INT_RGB);var output=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",output);byte[] photo=output.toByteArray();
 mvc.perform(put("/api/v1/me/photo").with(csrf()).contentType("image/png").content(photo)).andExpect(status().isUnauthorized());
 mvc.perform(put("/api/v1/me/photo").with(owner).contentType("image/png").content(photo)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/me/photo").with(owner).with(csrf()).contentType("image/png").content("<svg>unsafe</svg>")).andExpect(status().isUnprocessableEntity());
 mvc.perform(put("/api/v1/me/photo").with(owner).with(csrf()).contentType("image/png").content(new byte[2097153])).andExpect(status().isPayloadTooLarge());
 mvc.perform(put("/api/v1/me/photo").with(owner).with(csrf()).contentType("image/png").content(photo)).andExpect(status().isNoContent());
 byte[] stored=mvc.perform(get("/api/v1/me/photo").with(owner)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsByteArray();
 org.junit.jupiter.api.Assertions.assertEquals(512,javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(stored)).getWidth());
 mvc.perform(get("/api/v1/profiles/"+id+"/photo")).andExpect(status().isUnauthorized());
 mvc.perform(get("/api/v1/profiles/"+id+"/photo").with(other)).andExpect(status().isNotFound());
 mvc.perform(delete("/api/v1/me/photo").with(other).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/me/photo").with(owner)).andExpect(status().isOk());
 mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content("{\"memberVisible\":false,\"publicVisible\":true,\"version\":0}")).andExpect(status().isOk());
 mvc.perform(get("/api/v1/profiles/"+id+"/photo")).andExpect(status().isOk());
 database.update("insert into profile(subject,name) values ('photo-other','Other') on conflict do nothing");
 database.update("insert into member_block(blocker,blocked) values ('photo-other','photo-owner')");
 mvc.perform(get("/api/v1/profiles/"+id+"/photo").with(other)).andExpect(status().isNotFound());
 mvc.perform(delete("/api/v1/me/photo").with(owner).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/me/photo").with(owner)).andExpect(status().isNotFound());
 mvc.perform(get("/api/v1/profiles/"+id+"/photo")).andExpect(status().isNotFound());
 }

 @Test void anonymousProfilesRequireSeparatePublicConsent()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("public-owner"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 String body=mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andExpect(jsonPath("$.publicVisible").value(false)).andReturn().getResponse().getContentAsString();
 String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("publicId").asText();
 mvc.perform(get("/api/v1/profiles/"+id)).andExpect(status().isUnauthorized());
 mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"publicVisible\":true,\"version\":1}")).andExpect(status().isOk());
 mvc.perform(get("/api/v1/profiles/"+id)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.subject").doesNotExist());
 mvc.perform(get("/api/v1/me/visibility")).andExpect(status().isUnauthorized());
 mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"publicVisible\":false,\"version\":2}")).andExpect(status().isOk());
 mvc.perform(get("/api/v1/profiles/"+id)).andExpect(status().isUnauthorized());
 mvc.perform(get("/api/v1/profile-login/"+id)).andExpect(status().isFound()).andExpect(redirectedUrl("http://localhost:8081/oauth2/authorization/keycloak"));
 }

 @Test void privateProfilesRequireOptInAndRevocationIsImmediate()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("visibility-owner"));var other=oidcLogin().idToken(t->t.subject("visibility-other"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 String body=mvc.perform(get("/api/v1/me/visibility").with(owner)).andExpect(jsonPath("$.memberVisible").value(false)).andReturn().getResponse().getContentAsString();
 String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("publicId").asText();
 mvc.perform(get("/api/v1/profiles/"+id)).andExpect(status().isUnauthorized());
 mvc.perform(get("/api/v1/profiles/"+id).with(other)).andExpect(status().isNotFound());
 mvc.perform(get("/api/v1/profiles/"+id).with(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.subject").doesNotExist());
 String update="""
 {"memberVisible":true,"version":0}
 """;
 mvc.perform(put("/api/v1/me/visibility").with(owner).contentType("application/json").content(update)).andExpect(status().isForbidden());
 mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content(update)).andExpect(status().isOk());
 mvc.perform(get("/api/v1/profiles/"+id).with(other)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
 mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content(update)).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content(update.replace("true","false").replace(":0",":1"))).andExpect(status().isOk());
 mvc.perform(get("/api/v1/profiles/"+id).with(other)).andExpect(status().isNotFound());
 }

 @Test void connectionStateMachineProtectsParticipantsAndDuplicates()throws Exception{
 var sender=oidcLogin().idToken(t->t.subject("network-sender"));var recipient=oidcLogin().idToken(t->t.subject("network-recipient"));var outsider=oidcLogin().idToken(t->t.subject("network-outsider"));
 for(var user:java.util.List.of(sender,recipient))mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String visibility=mvc.perform(get("/api/v1/me/visibility").with(recipient)).andReturn().getResponse().getContentAsString();
 String recipientId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(visibility).get("publicId").asText();
 String requestId=java.util.UUID.randomUUID().toString();
 String invitation="""
 {"id":"%s","recipientId":"%s"}
 """.formatted(requestId,recipientId);
 mvc.perform(post("/api/v1/network").with(sender).with(csrf()).contentType("application/json").content(invitation)).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/me/visibility").with(recipient).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andExpect(status().isOk());
 mvc.perform(post("/api/v1/network").with(recipient).with(csrf()).contentType("application/json").content(invitation)).andExpect(status().isConflict());
 mvc.perform(post("/api/v1/network").with(sender).with(csrf()).contentType("application/json").content(invitation)).andExpect(status().isNoContent());
 mvc.perform(post("/api/v1/network").with(sender).with(csrf()).contentType("application/json").content(invitation.replace(requestId,java.util.UUID.randomUUID().toString()))).andExpect(status().isConflict());
 String accept="{\"action\":\"ACCEPT\",\"version\":0}";
 for(var user:java.util.List.of(sender,outsider))mvc.perform(put("/api/v1/network/"+requestId).with(user).with(csrf()).contentType("application/json").content(accept)).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/network/"+requestId).with(recipient).with(csrf()).contentType("application/json").content(accept)).andExpect(status().isNoContent());
 mvc.perform(put("/api/v1/network/"+requestId).with(recipient).with(csrf()).contentType("application/json").content(accept)).andExpect(status().isNoContent());
 org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from event_outbox where connection_id=?",Integer.class,java.util.UUID.fromString(requestId)));
 mvc.perform(post("/api/v1/network").with(sender).with(csrf()).contentType("application/json").content(invitation)).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/network").with(sender)).andExpect(jsonPath("$[0].state").value("ACCEPTED"));
 mvc.perform(get("/api/v1/network").with(outsider)).andExpect(jsonPath("$.length()").value(0));
 mvc.perform(put("/api/v1/network/"+requestId).with(sender).with(csrf()).contentType("application/json").content("{\"action\":\"REMOVE\",\"version\":1}")).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/network").with(recipient)).andExpect(jsonPath("$.length()").value(0));
 mvc.perform(put("/api/v1/network/"+requestId).with(recipient).with(csrf()).contentType("application/json").content(accept)).andExpect(status().isConflict());
 mvc.perform(put("/api/v1/network/"+requestId).with(sender).with(csrf()).contentType("application/json").content("{\"action\":\"REMOVE\",\"version\":1}")).andExpect(status().isNoContent());

 }

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
 void blocksCloseRelationshipsAndProtectBothDirections(boolean accepted)throws Exception{
 String suffix=java.util.UUID.randomUUID().toString();
 var a=oidcLogin().idToken(t->t.subject("block-a-"+suffix));var b=oidcLogin().idToken(t->t.subject("block-b-"+suffix));
 var users=java.util.List.of(a,b);var ids=new java.util.ArrayList<String>();
 for(var user:users){
 mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String body=mvc.perform(put("/api/v1/me/visibility").with(user).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
 ids.add(new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("publicId").asText());
 }
 String requestId=java.util.UUID.randomUUID().toString();
 String invitation="{\"id\":\"%s\",\"recipientId\":\"%s\"}".formatted(requestId,ids.get(1));
 mvc.perform(post("/api/v1/network").with(a).with(csrf()).contentType("application/json").content(invitation)).andExpect(status().isNoContent());
 if(accepted)mvc.perform(put("/api/v1/network/"+requestId).with(b).with(csrf()).contentType("application/json").content("{\"action\":\"ACCEPT\",\"version\":0}")).andExpect(status().isNoContent());
 String block="{\"memberId\":\"%s\"}".formatted(ids.get(1));
 mvc.perform(post("/api/v1/blocks").with(a).contentType("application/json").content(block)).andExpect(status().isForbidden());
 for(int repeat=0;repeat<2;repeat++)mvc.perform(post("/api/v1/blocks").with(a).with(csrf()).contentType("application/json").content(block)).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/blocks").with(a)).andExpect(jsonPath("$.length()").value(1));
 mvc.perform(get("/api/v1/blocks").with(b)).andExpect(jsonPath("$.length()").value(0));
 // The blocked member cannot remove the other member's block.
 mvc.perform(delete("/api/v1/blocks/"+ids.get(0)).with(b).with(csrf())).andExpect(status().isNoContent());
 for(int index=0;index<2;index++){
 var user=users.get(index);String target=ids.get(1-index);
 mvc.perform(get("/api/v1/network").with(user)).andExpect(jsonPath("$.length()").value(0));
 mvc.perform(get("/api/v1/profiles/"+target).with(user)).andExpect(status().isNotFound());
 mvc.perform(post("/api/v1/network").with(user).with(csrf()).contentType("application/json").content("{\"id\":\"%s\",\"recipientId\":\"%s\"}".formatted(java.util.UUID.randomUUID(),target))).andExpect(status().isConflict());
 }
 mvc.perform(put("/api/v1/network/"+requestId).with(b).with(csrf()).contentType("application/json").content("{\"action\":\"ACCEPT\",\"version\":0}")).andExpect(status().isConflict());
 mvc.perform(post("/api/v1/blocks").with(a).with(csrf()).contentType("application/json").content("{\"memberId\":\"%s\"}".formatted(ids.get(0)))).andExpect(status().isNotFound());
 mvc.perform(delete("/api/v1/blocks/"+ids.get(1)).with(a).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/network").with(a)).andExpect(jsonPath("$.length()").value(0));
 mvc.perform(get("/api/v1/profiles/"+ids.get(1)).with(a)).andExpect(status().isOk());
 mvc.perform(post("/api/v1/network").with(a).with(csrf()).contentType("application/json").content(invitation.replace(requestId,java.util.UUID.randomUUID().toString()))).andExpect(status().isNoContent());
 }

 @Test void metricsCannotBeReadThroughPublicApplicationPort()throws Exception{
 mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
 mvc.perform(get("/actuator/prometheus").with(oidcLogin())).andExpect(status().isForbidden());
 mvc.perform(get("/actuator/env").with(oidcLogin())).andExpect(status().isForbidden());
 }

 @Test void postOwnershipVisibilityAndDeletion()throws Exception{
 var a=oidcLogin().idToken(t->t.subject("post-owner"));var b=oidcLogin().idToken(t->t.subject("post-reader"));
 for(var user:java.util.List.of(a,b))mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String id=java.util.UUID.randomUUID().toString();String url="/api/v1/posts/"+id;
 String body="{\"text\":\"A professional update\",\"audience\":\"ONLY_ME\",\"version\":0}";
 mvc.perform(get("/api/v1/feed")).andExpect(status().isUnauthorized());
 mvc.perform(put(url).with(a).contentType("application/json").content(body)).andExpect(status().isForbidden());
 for(int repeat=0;repeat<2;repeat++)mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
 mvc.perform(get(url).with(b)).andExpect(status().isNotFound());
 mvc.perform(put(url).with(b).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 mvc.perform(delete(url+"?version=1").with(b).with(csrf())).andExpect(status().isConflict());
 String publish=body.replace("ONLY_ME","MEMBERS").replace(":0",":1");
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(publish)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
 mvc.perform(get(url).with(b)).andExpect(status().isOk()).andExpect(jsonPath("$.owned").value(false)).andExpect(header().string("Cache-Control","no-store"));
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(publish)).andExpect(status().isConflict());
 // Make the author summary discoverable to set up a block through its authorized API.
 String visibility=mvc.perform(put("/api/v1/me/visibility").with(a).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
 String authorId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(visibility).get("publicId").asText();
 mvc.perform(post("/api/v1/blocks").with(b).with(csrf()).contentType("application/json").content("{\"memberId\":\""+authorId+"\"}")).andExpect(status().isNoContent());
 mvc.perform(get(url).with(b)).andExpect(status().isNotFound());
 mvc.perform(get("/api/v1/feed").with(b)).andExpect(jsonPath("$.items[?(@.id == '"+id+"')]").isEmpty());
 mvc.perform(delete("/api/v1/blocks/"+authorId).with(b).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get(url).with(b)).andExpect(status().isOk());
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(body.replace(":0",":2"))).andExpect(status().isOk());
 mvc.perform(get(url).with(b)).andExpect(status().isNotFound());
 mvc.perform(delete(url+"?version=2").with(a).with(csrf())).andExpect(status().isConflict());
 mvc.perform(delete(url+"?version=3").with(a).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get(url).with(a)).andExpect(status().isNotFound());
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
 }
 @Test void feedPaginationAndPostValidation()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("post-pagination"));
 mvc.perform(get("/api/v1/me").with(owner)).andExpect(status().isOk());
 for(int i=0;i<21;i++)mvc.perform(put("/api/v1/posts/"+java.util.UUID.randomUUID()).with(owner).with(csrf()).contentType("application/json").content("{\"text\":\"Post "+i+"\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isOk());
 String first=mvc.perform(get("/api/v1/feed").with(owner)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
 var json=new com.fasterxml.jackson.databind.ObjectMapper();String cursor=json.readTree(first).get("nextCursor").asText();
 mvc.perform(get("/api/v1/feed?before="+cursor).with(owner)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].text").value("Post 0")).andExpect(jsonPath("$.nextCursor").isEmpty());
 mvc.perform(get("/api/v1/feed?before=-1").with(owner)).andExpect(status().isBadRequest());
 for(String invalid:java.util.List.of("{}","{\"text\":\"\",\"audience\":\"MEMBERS\",\"version\":0}","{\"text\":\"x\",\"audience\":\"PUBLIC\",\"version\":0}"))mvc.perform(put("/api/v1/posts/"+java.util.UUID.randomUUID()).with(owner).with(csrf()).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
 }

 @Test void concurrentPostCreationIsIdempotent()throws Exception{
 String subject="post-race";var user=oidcLogin().idToken(t->t.subject(subject));
 mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String id=java.util.UUID.randomUUID().toString();String body="{\"text\":\"One post\",\"audience\":\"ONLY_ME\",\"version\":0}";
 try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
 var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
 for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put("/api/v1/posts/"+id).with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content(body)).andReturn().getResponse().getStatus();}));
 gate.countDown();for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(15,java.util.concurrent.TimeUnit.SECONDS));
 }
 mvc.perform(get("/api/v1/feed").with(user)).andExpect(jsonPath("$.items[?(@.id == '"+id+"')]").value(org.hamcrest.Matchers.hasSize(1)));
 }

 @Test void reactionsRespectPrivacyOwnershipAndIdempotency()throws Exception{
 var owner=oidcLogin().idToken(t->t.subject("reaction-owner"));var reader=oidcLogin().idToken(t->t.subject("reaction-reader"));
 for(var user:java.util.List.of(owner,reader))mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String id=java.util.UUID.randomUUID().toString(),url="/api/v1/posts/"+id;
 String post="{\"text\":\"Reaction test\",\"audience\":\"ONLY_ME\",\"version\":0}";
 mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(post)).andExpect(status().isOk());
 mvc.perform(put(url+"/reaction").with(reader).with(csrf()).contentType("application/json").content("{\"liked\":true}")).andExpect(status().isNotFound());
 mvc.perform(put(url).with(owner).with(csrf()).contentType("application/json").content(post.replace("ONLY_ME","MEMBERS").replace(":0",":1"))).andExpect(status().isOk());
 mvc.perform(put(url+"/reaction").with(reader).contentType("application/json").content("{\"liked\":true}")).andExpect(status().isForbidden());
 mvc.perform(put(url+"/reaction").with(reader).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
 for(int i=0;i<2;i++)mvc.perform(put(url+"/reaction").with(reader).with(csrf()).contentType("application/json").content("{\"liked\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.reactionCount").value(1)).andExpect(jsonPath("$.liked").value(true)).andExpect(jsonPath("$.version").value(2));
 mvc.perform(get(url).with(owner)).andExpect(jsonPath("$.reactionCount").value(1)).andExpect(jsonPath("$.liked").value(false));
 mvc.perform(put(url+"/reaction").with(owner).with(csrf()).contentType("application/json").content("{\"liked\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.reactionCount").value(1));
 String visibility=mvc.perform(put("/api/v1/me/visibility").with(owner).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andReturn().getResponse().getContentAsString();
 String authorId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(visibility).get("publicId").asText();
 mvc.perform(post("/api/v1/blocks").with(reader).with(csrf()).contentType("application/json").content("{\"memberId\":\""+authorId+"\"}")).andExpect(status().isNoContent());
 mvc.perform(put(url+"/reaction").with(reader).with(csrf()).contentType("application/json").content("{\"liked\":false}")).andExpect(status().isNotFound());
 mvc.perform(delete("/api/v1/blocks/"+authorId).with(reader).with(csrf())).andExpect(status().isNoContent());
 for(int i=0;i<2;i++)mvc.perform(put(url+"/reaction").with(reader).with(csrf()).contentType("application/json").content("{\"liked\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.reactionCount").value(0));
 mvc.perform(delete(url+"?version=2").with(owner).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(put(url+"/reaction").with(reader).with(csrf()).contentType("application/json").content("{\"liked\":true}")).andExpect(status().isNotFound());
 }
 @Test void concurrentLikesCountOneMemberOnce()throws Exception{
 String subject="reaction-race";var user=oidcLogin().idToken(t->t.subject(subject));
 mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String url="/api/v1/posts/"+java.util.UUID.randomUUID();
 mvc.perform(put(url).with(user).with(csrf()).contentType("application/json").content("{\"text\":\"Reaction race\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isOk());
 try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
 var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
 for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put(url+"/reaction").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"liked\":true}")).andReturn().getResponse().getStatus();}));
 gate.countDown();for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(15,java.util.concurrent.TimeUnit.SECONDS));
 }
 mvc.perform(get(url).with(user)).andExpect(jsonPath("$.reactionCount").value(1)).andExpect(jsonPath("$.liked").value(true));
 }

 @Test void commentsEnforceParentPrivacyAuthorOwnershipAndCommenterBlocks()throws Exception{
 var a=oidcLogin().idToken(t->t.subject("comments-post-owner"));var b=oidcLogin().idToken(t->t.subject("comments-author"));var c=oidcLogin().idToken(t->t.subject("comments-reader"));
 for(var user:java.util.List.of(a,b,c))mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String postUrl="/api/v1/posts/"+java.util.UUID.randomUUID(),list=postUrl+"/comments",comment=list+"/"+java.util.UUID.randomUUID();
 String postBody="{\"text\":\"Comment target\",\"audience\":\"MEMBERS\",\"version\":0}";
 mvc.perform(put(postUrl).with(a).with(csrf()).contentType("application/json").content(postBody)).andExpect(status().isOk());
 mvc.perform(get(list)).andExpect(status().isUnauthorized());
 mvc.perform(put(comment).with(b).contentType("application/json").content("{\"text\":\"Useful perspective\"}")).andExpect(status().isForbidden());
 for(int i=0;i<2;i++)mvc.perform(put(comment).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"Useful perspective\"}")).andExpect(status().isOk());
 mvc.perform(put(comment).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"Different payload\"}")).andExpect(status().isConflict());
 mvc.perform(get(list).with(c)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].owned").value(false)).andExpect(header().string("Cache-Control","no-store"));
 mvc.perform(delete(comment).with(a).with(csrf())).andExpect(status().isNotFound());
 String visibility=mvc.perform(put("/api/v1/me/visibility").with(b).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andReturn().getResponse().getContentAsString();
 String authorId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(visibility).get("publicId").asText();
 mvc.perform(post("/api/v1/blocks").with(c).with(csrf()).contentType("application/json").content("{\"memberId\":\""+authorId+"\"}")).andExpect(status().isNoContent());
 mvc.perform(get(postUrl).with(c)).andExpect(status().isOk());
 mvc.perform(get(list).with(c)).andExpect(jsonPath("$.items.length()").value(0));
 mvc.perform(get(list).with(a)).andExpect(jsonPath("$.items.length()").value(1));
 mvc.perform(put(postUrl).with(a).with(csrf()).contentType("application/json").content(postBody.replace("MEMBERS","ONLY_ME").replace(":0",":1"))).andExpect(status().isOk());
 mvc.perform(get(list).with(b)).andExpect(status().isNotFound());
 mvc.perform(put(list+"/"+java.util.UUID.randomUUID()).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"No access\"}")).andExpect(status().isNotFound());
 mvc.perform(put(postUrl).with(a).with(csrf()).contentType("application/json").content(postBody.replace(":0",":2"))).andExpect(status().isOk());
 for(int i=0;i<2;i++)mvc.perform(delete(comment).with(b).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(put(comment).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"Useful perspective\"}")).andExpect(status().isConflict());
 mvc.perform(get(list).with(a)).andExpect(jsonPath("$.items.length()").value(0));
 mvc.perform(delete(postUrl+"?version=3").with(a).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get(list).with(a)).andExpect(status().isNotFound());
 }
 @Test void concurrentCommentRetriesAndPagination()throws Exception{
 String subject="comment-race";var user=oidcLogin().idToken(t->t.subject(subject));
 mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String url="/api/v1/posts/"+java.util.UUID.randomUUID();
 mvc.perform(put(url).with(user).with(csrf()).contentType("application/json").content("{\"text\":\"Comments\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isOk());
 String comment=url+"/comments/"+java.util.UUID.randomUUID();
 try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
 var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
 for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put(comment).with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"text\":\"One comment\"}")).andReturn().getResponse().getStatus();}));
 gate.countDown();for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(15,java.util.concurrent.TimeUnit.SECONDS));
 }
 mvc.perform(get(url+"/comments").with(user)).andExpect(jsonPath("$.items.length()").value(1));
 for(int i=0;i<20;i++)mvc.perform(put(url+"/comments/"+java.util.UUID.randomUUID()).with(user).with(csrf()).contentType("application/json").content("{\"text\":\"Comment "+i+"\"}")).andExpect(status().isOk());
 String result=mvc.perform(get(url+"/comments").with(user)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
 String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).get("nextCursor").asText();
 mvc.perform(get(url+"/comments?before="+cursor).with(user)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].text").value("One comment"));
 for(String body:java.util.List.of("{}","{\"text\":\"   \"}","{\"text\":\""+"x".repeat(2001)+"\"}"))mvc.perform(put(url+"/comments/"+java.util.UUID.randomUUID()).with(user).with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
 mvc.perform(get(url+"/comments?before=0").with(user)).andExpect(status().isBadRequest());
 }

 @Test void savedPostsStayPrivateAndNeverBypassVisibility()throws Exception{
 var a=oidcLogin().idToken(t->t.subject("bookmark-author"));var b=oidcLogin().idToken(t->t.subject("bookmark-reader"));
 for(var user:java.util.List.of(a,b))mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String url="/api/v1/posts/"+java.util.UUID.randomUUID();String body="{\"text\":\"Bookmark me\",\"audience\":\"MEMBERS\",\"version\":0}";
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
 mvc.perform(get("/api/v1/saved-posts")).andExpect(status().isUnauthorized());
 mvc.perform(put(url+"/saved").with(b).contentType("application/json").content("{\"saved\":true}")).andExpect(status().isForbidden());
 mvc.perform(put(url+"/saved").with(b).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
 for(int i=0;i<2;i++)mvc.perform(put(url+"/saved").with(b).with(csrf()).contentType("application/json").content("{\"saved\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.version").value(1));
 mvc.perform(get("/api/v1/saved-posts").with(b)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(header().string("Cache-Control","no-store"));
 mvc.perform(get("/api/v1/saved-posts").with(a)).andExpect(jsonPath("$.items.length()").value(0));
 mvc.perform(put(url+"/saved").with(a).with(csrf()).contentType("application/json").content("{\"saved\":false}")).andExpect(status().isOk());
 mvc.perform(get(url).with(b)).andExpect(jsonPath("$.saved").value(true));
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(body.replace("MEMBERS","ONLY_ME").replace(":0",":1"))).andExpect(status().isOk());
 mvc.perform(get("/api/v1/saved-posts").with(b)).andExpect(jsonPath("$.items.length()").value(0));
 mvc.perform(put(url+"/saved").with(b).with(csrf()).contentType("application/json").content("{\"saved\":true}")).andExpect(status().isNotFound());
 mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content(body.replace(":0",":2"))).andExpect(status().isOk());
 String visibility=mvc.perform(put("/api/v1/me/visibility").with(a).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andReturn().getResponse().getContentAsString();
 String authorId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(visibility).get("publicId").asText();
 mvc.perform(post("/api/v1/blocks").with(b).with(csrf()).contentType("application/json").content("{\"memberId\":\""+authorId+"\"}")).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/saved-posts").with(b)).andExpect(jsonPath("$.items.length()").value(0));
 mvc.perform(delete("/api/v1/blocks/"+authorId).with(b).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/saved-posts").with(b)).andExpect(jsonPath("$.items.length()").value(1));
 for(int i=0;i<2;i++)mvc.perform(put(url+"/saved").with(b).with(csrf()).contentType("application/json").content("{\"saved\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
 mvc.perform(put(url+"/saved").with(b).with(csrf()).contentType("application/json").content("{\"saved\":true}")).andExpect(status().isOk());
 mvc.perform(delete(url+"?version=3").with(a).with(csrf())).andExpect(status().isNoContent());
 mvc.perform(get("/api/v1/saved-posts").with(b)).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test void concurrentBookmarkRetriesAndSavedPagination()throws Exception{
 String subject="bookmark-race";var user=oidcLogin().idToken(t->t.subject(subject));mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
 String first="/api/v1/posts/"+java.util.UUID.randomUUID();
 mvc.perform(put(first).with(user).with(csrf()).contentType("application/json").content("{\"text\":\"First bookmark\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isOk());
 try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
 var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
 for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put(first+"/saved").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"saved\":true}")).andReturn().getResponse().getStatus();}));
 gate.countDown();for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(15,java.util.concurrent.TimeUnit.SECONDS));
 }
 mvc.perform(get("/api/v1/saved-posts").with(user)).andExpect(jsonPath("$.items.length()").value(1));
 for(int i=0;i<20;i++){String url="/api/v1/posts/"+java.util.UUID.randomUUID();mvc.perform(put(url).with(user).with(csrf()).contentType("application/json").content("{\"text\":\"Later bookmark\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isOk());mvc.perform(put(url+"/saved").with(user).with(csrf()).contentType("application/json").content("{\"saved\":true}")).andExpect(status().isOk());}
 String result=mvc.perform(get("/api/v1/saved-posts").with(user)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
 String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).get("nextCursor").asText();
 mvc.perform(get("/api/v1/saved-posts?before="+cursor).with(user)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].text").value("First bookmark"));
 mvc.perform(get("/api/v1/saved-posts?before=0").with(user)).andExpect(status().isBadRequest());
 }

 String followProfile(String subject)throws Exception{
  var user=oidcLogin().idToken(t->t.subject(subject));
  mvc.perform(get("/api/v1/me").with(user)).andExpect(status().isOk());
  String result=mvc.perform(put("/api/v1/me/visibility").with(user).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":0}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  return new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).get("publicId").asText();
 }
 @Test void followingPrivacyAuthorizationAndRemoval()throws Exception{
  String aId=followProfile("follow-a"),bId=followProfile("follow-b");
  var a=oidcLogin().idToken(t->t.subject("follow-a"));var b=oidcLogin().idToken(t->t.subject("follow-b"));
  String url="/api/v1/following/"+bId;
  mvc.perform(get("/api/v1/following")).andExpect(status().isUnauthorized());
  mvc.perform(put(url).with(a).contentType("application/json").content("{\"following\":true}")).andExpect(status().isForbidden());
  mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/v1/following/"+aId).with(a).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isNotFound());
  for(int i=0;i<2;i++)mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.following").value(true));
  mvc.perform(get("/api/v1/following").with(a)).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/following").with(b)).andExpect(jsonPath("$.total").value(0));
  mvc.perform(get("/api/v1/network").with(a)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(put("/api/v1/me/visibility").with(b).with(csrf()).contentType("application/json").content("{\"memberVisible\":false,\"version\":1}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/following").with(a)).andExpect(jsonPath("$.items[0].name").value("Private member")).andExpect(jsonPath("$.items[0].visible").value(false));
  mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isNotFound());
  for(int i=0;i<2;i++)mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{\"following\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/following").with(a)).andExpect(jsonPath("$.total").value(0));
  mvc.perform(get(url).with(a)).andExpect(status().isNotFound());
 }
 @Test void followingPaginationConcurrentRetriesAndBlocks()throws Exception{
  String subject="follow-race",self=followProfile(subject),target=followProfile("follow-target");var user=oidcLogin().idToken(t->t.subject(subject));
  String url="/api/v1/following/"+target;
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
   for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put(url).with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"following\":true}")).andReturn().getResponse().getStatus();}));
   gate.countDown();for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(15,java.util.concurrent.TimeUnit.SECONDS));
  }
  mvc.perform(get("/api/v1/following").with(user)).andExpect(jsonPath("$.total").value(1));
  for(int i=0;i<20;i++){String id=followProfile("follow-page-"+i);mvc.perform(put("/api/v1/following/"+id).with(user).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk());}
  String result=mvc.perform(get("/api/v1/following").with(user)).andExpect(jsonPath("$.items.length()").value(20)).andExpect(jsonPath("$.total").value(21)).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).get("nextCursor").asText();
  mvc.perform(get("/api/v1/following?before="+cursor).with(user)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].memberId").value(target));
  mvc.perform(get("/api/v1/following?before=0").with(user)).andExpect(status().isBadRequest());
  var other=oidcLogin().idToken(t->t.subject("follow-target"));
  mvc.perform(put("/api/v1/following/"+self).with(other).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk());
  // Even a target that became private can be blocked through an existing follow.
  mvc.perform(put("/api/v1/me/visibility").with(other).with(csrf()).contentType("application/json").content("{\"memberVisible\":false,\"version\":1}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/blocks").with(user).with(csrf()).contentType("application/json").content("{\"memberId\":\""+target+"\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/following").with(user)).andExpect(jsonPath("$.total").value(20));
  mvc.perform(get("/api/v1/following").with(other)).andExpect(jsonPath("$.total").value(0));
  mvc.perform(put("/api/v1/following/"+self).with(other).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isNotFound());
  mvc.perform(delete("/api/v1/blocks/"+target).with(user).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/following").with(other)).andExpect(jsonPath("$.total").value(0));
 }
 @Test void concurrentFollowAndBlockCannotRestoreRelationship()throws Exception{
  String subject="follow-block-race",id=followProfile("follow-block-target");followProfile(subject);
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new java.util.concurrent.CountDownLatch(1);
   var follow=executor.submit(()->{gate.await();return mvc.perform(put("/api/v1/following/"+id).with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"following\":true}")).andReturn().getResponse().getStatus();});
   var block=executor.submit(()->{gate.await();return mvc.perform(post("/api/v1/blocks").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"memberId\":\""+id+"\"}")).andReturn().getResponse().getStatus();});
   gate.countDown();int status=follow.get(15,java.util.concurrent.TimeUnit.SECONDS);org.junit.jupiter.api.Assertions.assertTrue(status==200||status==404);org.junit.jupiter.api.Assertions.assertEquals(204,block.get(15,java.util.concurrent.TimeUnit.SECONDS));
  }
  mvc.perform(get("/api/v1/following").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(jsonPath("$.total").value(0));
 }

 @Test @org.springframework.transaction.annotation.Transactional void followedFeedUsesCurrentGraphAndPostVisibility()throws Exception{
  String reader="feed-follow-reader",author="feed-follow-author";String authorId=followProfile(author);followProfile(reader);followProfile("feed-follow-stranger");
  var r=oidcLogin().idToken(t->t.subject(reader));var a=oidcLogin().idToken(t->t.subject(author));
  String id=java.util.UUID.randomUUID().toString(),url="/api/v1/posts/"+id;
  mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Followed update\",\"audience\":\"MEMBERS\",\"version\":0}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/posts/"+java.util.UUID.randomUUID()).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Private update\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/posts/"+java.util.UUID.randomUUID()).with(oidcLogin().idToken(t->t.subject("feed-follow-stranger"))).with(csrf()).contentType("application/json").content("{\"text\":\"Unfollowed update\",\"audience\":\"MEMBERS\",\"version\":0}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/following/"+authorId).with(r).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(id));
  // Profile privacy and post audience are separate policies.
  mvc.perform(put("/api/v1/me/visibility").with(a).with(csrf()).contentType("application/json").content("{\"memberVisible\":false,\"version\":1}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Followed update\",\"audience\":\"ONLY_ME\",\"version\":1}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(url).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Followed update\",\"audience\":\"MEMBERS\",\"version\":2}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/following/"+authorId).with(r).with(csrf()).contentType("application/json").content("{\"following\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/me/visibility").with(a).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":2}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/following/"+authorId).with(r).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/blocks").with(r).with(csrf()).contentType("application/json").content("{\"memberId\":\""+authorId+"\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(delete("/api/v1/blocks/"+authorId).with(r).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(put("/api/v1/following/"+authorId).with(r).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk());
  mvc.perform(delete(url+"?version=3").with(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/feed?scope=UNKNOWN").with(r)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING")).andExpect(status().isUnauthorized());
 }
 @Test @org.springframework.transaction.annotation.Transactional void followedFeedPaginationDoesNotSkipOrDuplicatePosts()throws Exception{
  String author="feed-page-author",reader="feed-page-reader",id=followProfile(author);followProfile(reader);
  var a=oidcLogin().idToken(t->t.subject(author));var r=oidcLogin().idToken(t->t.subject(reader));
  mvc.perform(put("/api/v1/following/"+id).with(r).with(csrf()).contentType("application/json").content("{\"following\":true}")).andExpect(status().isOk());
  String oldest=java.util.UUID.randomUUID().toString();
  for(int i=0;i<21;i++)mvc.perform(put("/api/v1/posts/"+(i==0?oldest:java.util.UUID.randomUUID())).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Page update "+i+"\",\"audience\":\"MEMBERS\",\"version\":0}")).andExpect(status().isOk());
  String response=mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("nextCursor").asText();
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING&before="+cursor).with(r)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(oldest)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING&before=0").with(r)).andExpect(status().isBadRequest());
 }

 @Test @org.springframework.transaction.annotation.Transactional void postSearchFiltersPrivateDeletedBlockedAndPaginatesLiteralQueries()throws Exception{
  String author="search-post-author",reader="search-post-reader",authorId=followProfile(author);followProfile(reader);
  var a=oidcLogin().idToken(t->t.subject(author));var r=oidcLogin().idToken(t->t.subject(reader));
  for(int i=0;i<21;i++)database.update("insert into content_post(id,author,body,audience) values(?,?,?,?)",java.util.UUID.randomUUID(),author,"Needle100%_literal "+i,"MEMBERS");
  database.update("insert into content_post(id,author,body,audience) values(?,?,?,?)",java.util.UUID.randomUUID(),author,"Needle100%_literal private","ONLY_ME");
  database.update("insert into content_post(id,author,body,audience,deleted_at) values(?,?,?,'MEMBERS',now())",java.util.UUID.randomUUID(),author,"Needle100%_literal deleted");
  mvc.perform(get("/api/v1/search/posts").param("q","Needle100%_literal")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/search/posts").param("q","x").with(r)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/search/posts").param("q","Needle").param("before","0").with(r)).andExpect(status().isBadRequest());
  String body=mvc.perform(get("/api/v1/search/posts").param("q","needle100%_literal").with(r)).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
  mvc.perform(get("/api/v1/search/posts").param("q","needle100%_literal").param("before",cursor).with(r)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/search/posts").param("q","_literal private").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/search/posts").param("q","_literal private").with(a)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/search/posts").param("q","_literal deleted").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(post("/api/v1/blocks").with(r).with(csrf()).contentType("application/json").content("{\"memberId\":\""+authorId+"\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/search/posts").param("q","needle100%_literal").with(r)).andExpect(jsonPath("$.items.length()").value(0));
 }

 @Test @org.springframework.transaction.annotation.Transactional void peopleSearchRequiresOptInAndEnforcesPrivacy()throws Exception{
  String id=followProfile("discover-target");followProfile("discover-reader");
  var target=oidcLogin().idToken(t->t.subject("discover-target"));var reader=oidcLogin().idToken(t->t.subject("discover-reader"));
  mvc.perform(put("/api/v1/me").with(target).with(csrf()).contentType("application/json").content("{\"name\":\"OrionDiscovery Person\",\"headline\":\"Engineer 100% _literal\",\"location\":\"Bengaluru\",\"about\":\"Private search body\",\"version\":0}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/me/discovery").with(target)).andExpect(jsonPath("$.discoverable").value(false));
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery").with(reader)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/me/discovery").with(target).contentType("application/json").content("{\"discoverable\":true,\"version\":0}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/me/discovery").with(target).with(csrf()).contentType("application/json").content("{\"discoverable\":true,\"version\":0}")).andExpect(status().isOk());
  mvc.perform(put("/api/v1/me/discovery").with(target).with(csrf()).contentType("application/json").content("{\"discoverable\":false,\"version\":0}")).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/search/people").param("q","oriondiscovery").param("location","BENGAL").with(reader)).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].memberId").value(id)).andExpect(jsonPath("$.items[0].about").doesNotExist()).andExpect(jsonPath("$.items[0].subject").doesNotExist());
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery&location=London").with(reader)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery").with(target)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/search/people").param("q","100%").with(reader)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/search/people").param("q","%' OR true --").with(reader)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/me/visibility").with(target).with(csrf()).contentType("application/json").content("{\"memberVisible\":false,\"version\":1}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery").with(reader)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/me/visibility").with(target).with(csrf()).contentType("application/json").content("{\"memberVisible\":true,\"version\":2}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/blocks").with(reader).with(csrf()).contentType("application/json").content("{\"memberId\":\""+id+"\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery").with(reader)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(delete("/api/v1/blocks/"+id).with(reader).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(put("/api/v1/me/discovery").with(target).with(csrf()).contentType("application/json").content("{\"discoverable\":false,\"version\":1}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/search/people?q=OrionDiscovery").with(reader)).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test @org.springframework.transaction.annotation.Transactional void peopleSearchPaginationAndValidation()throws Exception{
  followProfile("search-page-reader");var reader=oidcLogin().idToken(t->t.subject("search-page-reader"));
  var ids=new java.util.HashSet<String>();
  for(int i=0;i<21;i++){
   String subject="search-page-"+i;ids.add(followProfile(subject));var person=oidcLogin().idToken(t->t.subject(subject));
   mvc.perform(put("/api/v1/me").with(person).with(csrf()).contentType("application/json").content("{\"name\":\"SearchPagination "+i+"\",\"headline\":\"Engineer\",\"location\":\"Test city\",\"about\":\"\",\"version\":0}")).andExpect(status().isOk());
   mvc.perform(put("/api/v1/me/discovery").with(person).with(csrf()).contentType("application/json").content("{\"discoverable\":true,\"version\":0}")).andExpect(status().isOk());
  }
  var json=new com.fasterxml.jackson.databind.ObjectMapper();
  var first=json.readTree(mvc.perform(get("/api/v1/search/people?q=SearchPagination").with(reader)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString());
  var second=json.readTree(mvc.perform(get("/api/v1/search/people?q=SearchPagination&after="+first.get("nextCursor").asText()).with(reader)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty()).andReturn().getResponse().getContentAsString());
  for(var item:first.get("items"))org.junit.jupiter.api.Assertions.assertTrue(ids.remove(item.get("memberId").asText()));
  for(var item:second.get("items"))org.junit.jupiter.api.Assertions.assertTrue(ids.remove(item.get("memberId").asText()));
  org.junit.jupiter.api.Assertions.assertTrue(ids.isEmpty());
  for(String query:java.util.List.of(""," ","x","x".repeat(81)))mvc.perform(get("/api/v1/search/people").param("q",query).with(reader)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/search/people?q=SearchPagination&after=invalid").with(reader)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/search/people").param("q","SearchPagination").param("location","x".repeat(121)).with(reader)).andExpect(status().isBadRequest());
  mvc.perform(put("/api/v1/me/discovery").with(reader).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
 }

 record ChatFixture(String id,String aId,String bId,String connection){}
 ChatFixture chat(String prefix)throws Exception{
  String aId=followProfile(prefix+"-a"),bId=followProfile(prefix+"-b"),connection=java.util.UUID.randomUUID().toString();
  var a=oidcLogin().idToken(t->t.subject(prefix+"-a"));var b=oidcLogin().idToken(t->t.subject(prefix+"-b"));
  mvc.perform(post("/api/v1/network").with(a).with(csrf()).contentType("application/json").content("{\"id\":\""+connection+"\",\"recipientId\":\""+bId+"\"}")).andExpect(status().isNoContent());
  mvc.perform(put("/api/v1/network/"+connection).with(b).with(csrf()).contentType("application/json").content("{\"action\":\"ACCEPT\",\"version\":0}")).andExpect(status().isNoContent());
  String response=mvc.perform(post("/api/v1/conversations").with(a).with(csrf()).contentType("application/json").content("{\"memberId\":\""+bId+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
  mvc.perform(post("/api/v1/conversations").with(b).with(csrf()).contentType("application/json").content("{\"memberId\":\""+aId+"\"}")).andExpect(jsonPath("$.id").value(id));
  return new ChatFixture(id,aId,bId,connection);
 }
 @Test void directMessagesPersistAuthorizeDeduplicateAndTrackReads()throws Exception{
  var chat=chat("chat-policy");var a=oidcLogin().idToken(t->t.subject("chat-policy-a"));var b=oidcLogin().idToken(t->t.subject("chat-policy-b"));var stranger=oidcLogin().idToken(t->t.subject("chat-stranger"));
  String base="/api/v1/conversations/"+chat.id(),message=base+"/messages/"+java.util.UUID.randomUUID();
  mvc.perform(get("/api/v1/conversations")).andExpect(status().isUnauthorized());
  mvc.perform(get(base+"/messages").with(stranger)).andExpect(status().isNotFound());
  mvc.perform(put(message).with(stranger).with(csrf()).contentType("application/json").content("{\"text\":\"Denied\"}")).andExpect(status().isNotFound());
  mvc.perform(put(message).with(a).contentType("application/json").content("{\"text\":\"Hello\"}")).andExpect(status().isForbidden());
  for(String text:java.util.List.of(""," ","x".repeat(3001)))mvc.perform(put(message).with(a).with(csrf()).contentType("application/json").content("{\"text\":\""+text+"\"}")).andExpect(status().isBadRequest());
  for(int n=0;n<2;n++)mvc.perform(put(message).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Hello\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.sequence").value(1));
  mvc.perform(put(message).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"Changed\"}")).andExpect(status().isConflict());
  mvc.perform(put(message).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"Hello\"}")).andExpect(status().isConflict());
  mvc.perform(get(base+"/messages").with(b)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].text").value("Hello")).andExpect(jsonPath("$.items[0].owned").value(false));
  mvc.perform(get("/api/v1/conversations").with(b)).andExpect(jsonPath("$.items[0].unreadCount").value(1));
  mvc.perform(put(base+"/read").with(b).with(csrf()).contentType("application/json").content("{\"throughSequence\":2}")).andExpect(status().isBadRequest());
  for(int n:java.util.List.of(1,1,0))mvc.perform(put(base+"/read").with(b).with(csrf()).contentType("application/json").content("{\"throughSequence\":"+n+"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/conversations").with(b)).andExpect(jsonPath("$.items[0].unreadCount").value(0));
  mvc.perform(get(base+"/messages").with(a)).andExpect(jsonPath("$.items[0].read").value(true));
  mvc.perform(put("/api/v1/network/"+chat.connection()).with(a).with(csrf()).contentType("application/json").content("{\"action\":\"REMOVE\",\"version\":1}")).andExpect(status().isNoContent());
  mvc.perform(get(base+"/messages").with(a)).andExpect(jsonPath("$.canSend").value(false)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(put(base+"/messages/"+java.util.UUID.randomUUID()).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"No connection\"}")).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/blocks").with(a).with(csrf()).contentType("application/json").content("{\"memberId\":\""+chat.bId()+"\"}")).andExpect(status().isNoContent());
  mvc.perform(get(base+"/messages").with(a)).andExpect(status().isNotFound());mvc.perform(get(base+"/messages").with(b)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/conversations").with(b)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(delete("/api/v1/blocks/"+chat.bId()).with(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(base+"/messages").with(a)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.canSend").value(false));
 }
 @Test void concurrentMessageRetriesPreserveOrderAndHistoryPagination()throws Exception{
  var chat=chat("chat-order");String base="/api/v1/conversations/"+chat.id(),message=base+"/messages/"+java.util.UUID.randomUUID();
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
   for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put(message).with(oidcLogin().idToken(t->t.subject("chat-order-a"))).with(csrf()).contentType("application/json").content("{\"text\":\"Retry once\"}")).andReturn().getResponse().getStatus();}));
   gate.countDown();for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(15,java.util.concurrent.TimeUnit.SECONDS));
  }
  var a=oidcLogin().idToken(t->t.subject("chat-order-a"));
  mvc.perform(get(base+"/messages").with(a)).andExpect(jsonPath("$.latestSequence").value(1));
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
   for(int i=0;i<50;i++){String subject=i%2==0?"chat-order-a":"chat-order-b";futures.add(executor.submit(()->mvc.perform(put(base+"/messages/"+java.util.UUID.randomUUID()).with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"text\":\"Concurrent send\"}")).andReturn().getResponse().getStatus()));}
   for(var future:futures)org.junit.jupiter.api.Assertions.assertEquals(200,future.get(20,java.util.concurrent.TimeUnit.SECONDS));
  }
  var tree=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get(base+"/messages").with(a)).andExpect(jsonPath("$.items.length()").value(50)).andExpect(jsonPath("$.latestSequence").value(51)).andReturn().getResponse().getContentAsString());
  long seq=2;for(var item:tree.get("items"))org.junit.jupiter.api.Assertions.assertEquals(seq++,item.get("sequence").asLong());
  mvc.perform(get(base+"/messages?before="+tree.get("nextCursor").asText()).with(a)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].sequence").value(1));
  mvc.perform(get(base+"/messages?before=0").with(a)).andExpect(status().isBadRequest());
 }
 @Test void concurrentBlockAndSendCannotBypassBlock()throws Exception{
  var chat=chat("chat-block");String base="/api/v1/conversations/"+chat.id();
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new java.util.concurrent.CountDownLatch(1);
   var send=executor.submit(()->{gate.await();return mvc.perform(put(base+"/messages/"+java.util.UUID.randomUUID()).with(oidcLogin().idToken(t->t.subject("chat-block-a"))).with(csrf()).contentType("application/json").content("{\"text\":\"Race\"}")).andReturn().getResponse().getStatus();});
   var block=executor.submit(()->{gate.await();return mvc.perform(post("/api/v1/blocks").with(oidcLogin().idToken(t->t.subject("chat-block-b"))).with(csrf()).contentType("application/json").content("{\"memberId\":\""+chat.aId()+"\"}")).andReturn().getResponse().getStatus();});
   gate.countDown();int code=send.get(15,java.util.concurrent.TimeUnit.SECONDS);org.junit.jupiter.api.Assertions.assertTrue(code==200||code==404);org.junit.jupiter.api.Assertions.assertEquals(204,block.get(15,java.util.concurrent.TimeUnit.SECONDS));
  }
  mvc.perform(get(base+"/messages").with(oidcLogin().idToken(t->t.subject("chat-block-a")))).andExpect(status().isNotFound());
 }

 @Test void conversationPreferencesArePrivateVersionedAndNewMessagesRestoreInbox()throws Exception{
  var chat=chat("chat-prefs");var a=oidcLogin().idToken(t->t.subject("chat-prefs-a"));var b=oidcLogin().idToken(t->t.subject("chat-prefs-b"));
  String base="/api/v1/conversations/"+chat.id(),message=base+"/messages/"+java.util.UUID.randomUUID();
  mvc.perform(put(message).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"Original\"}")).andExpect(status().isOk());
  String desired="{\"starred\":true,\"archived\":true,\"version\":0}";
  mvc.perform(put(base+"/preferences").with(a).contentType("application/json").content(desired)).andExpect(status().isForbidden());
  mvc.perform(put(base+"/preferences").with(oidcLogin().idToken(t->t.subject("unrelated"))).with(csrf()).contentType("application/json").content(desired)).andExpect(status().isNotFound());
  mvc.perform(put(base+"/preferences").with(a).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
  mvc.perform(put(base+"/preferences").with(a).with(csrf()).contentType("application/json").content(desired)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(get("/api/v1/conversations").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/conversations?view=ARCHIVED").with(a)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].unreadCount").value(1));
  mvc.perform(get("/api/v1/conversations?view=STARRED").with(a)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/conversations").with(b)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].preferences.starred").value(false));
  mvc.perform(get("/api/v1/conversations?view=STARRED").with(b)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put(base+"/preferences").with(b).with(csrf()).contentType("application/json").content("{\"starred\":false,\"archived\":true,\"version\":0}")).andExpect(status().isOk());
  mvc.perform(put(message).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"Original\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/conversations?view=ARCHIVED").with(a)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get("/api/v1/conversations?view=ARCHIVED").with(b)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(put(base+"/messages/"+java.util.UUID.randomUUID()).with(b).with(csrf()).contentType("application/json").content("{\"text\":\"New reply\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/conversations").with(a)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].preferences.archived").value(false)).andExpect(jsonPath("$.items[0].preferences.starred").value(true)).andExpect(jsonPath("$.items[0].preferences.version").value(2)).andExpect(jsonPath("$.items[0].unreadCount").value(2));
  mvc.perform(get("/api/v1/conversations").with(b)).andExpect(jsonPath("$.items[0].preferences.version").value(2));
  mvc.perform(put(base+"/preferences").with(a).with(csrf()).contentType("application/json").content(desired.replace(":0",":1"))).andExpect(status().isConflict());
  mvc.perform(put(base+"/preferences").with(a).with(csrf()).contentType("application/json").content("{\"starred\":false,\"archived\":false,\"version\":2}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/conversations?view=STARRED").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/conversations?view=UNKNOWN").with(a)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/blocks").with(a).with(csrf()).contentType("application/json").content("{\"memberId\":\""+chat.bId()+"\"}")).andExpect(status().isNoContent());
  mvc.perform(put(base+"/preferences").with(a).with(csrf()).contentType("application/json").content("{\"starred\":true,\"archived\":false,\"version\":3}")).andExpect(status().isNotFound());
 }
 @Test void concurrentConversationPreferencesRejectStaleWriters()throws Exception{
  var chat=chat("chat-prefs-race");
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new java.util.concurrent.CountDownLatch(1);var futures=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
   for(int i=0;i<8;i++)futures.add(executor.submit(()->{gate.await();return mvc.perform(put("/api/v1/conversations/"+chat.id()+"/preferences").with(oidcLogin().idToken(t->t.subject("chat-prefs-race-a"))).with(csrf()).contentType("application/json").content("{\"starred\":true,\"archived\":false,\"version\":0}")).andReturn().getResponse().getStatus();}));
   gate.countDown();int winners=0;for(var future:futures){int status=future.get(15,java.util.concurrent.TimeUnit.SECONDS);org.junit.jupiter.api.Assertions.assertTrue(status==200||status==409);if(status==200)winners++;}
   org.junit.jupiter.api.Assertions.assertEquals(1,winners);
  }
  mvc.perform(get("/api/v1/conversations/"+chat.id()+"/messages").with(oidcLogin().idToken(t->t.subject("chat-prefs-race-a")))).andExpect(jsonPath("$.preferences.version").value(1)).andExpect(jsonPath("$.preferences.starred").value(true));
 }

 @Test @org.springframework.transaction.annotation.Transactional void conversationSearchFiltersBeforePaginationAndKeepsParticipantBoundary()throws Exception{
  followProfile("conversation-search-a");var a=oidcLogin().idToken(t->t.subject("conversation-search-a"));
  String first=null,firstMember=null;
  for(int i=0;i<21;i++){
   String subject="conversation-search-"+i,id=followProfile(subject);var b=oidcLogin().idToken(t->t.subject(subject));
   mvc.perform(put("/api/v1/me").with(b).with(csrf()).contentType("application/json").content("{\"name\":\"SearchChats "+i+(i==0?" 100% _literal":"")+"\",\"headline\":\"\",\"location\":\"\",\"about\":\"\",\"version\":0}")).andExpect(status().isOk());
   String invitation=java.util.UUID.randomUUID().toString();
   mvc.perform(post("/api/v1/network").with(a).with(csrf()).contentType("application/json").content("{\"id\":\""+invitation+"\",\"recipientId\":\""+id+"\"}")).andExpect(status().isNoContent());
   mvc.perform(put("/api/v1/network/"+invitation).with(b).with(csrf()).contentType("application/json").content("{\"action\":\"ACCEPT\",\"version\":0}")).andExpect(status().isNoContent());
   String result=mvc.perform(post("/api/v1/conversations").with(a).with(csrf()).contentType("application/json").content("{\"memberId\":\""+id+"\"}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   if(i==0){first=new com.fasterxml.jackson.databind.ObjectMapper().readTree(result).get("id").asText();firstMember=id;}
  }
  var firstPage=new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get("/api/v1/conversations").param("q","searchchats").with(a)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString());
  mvc.perform(get("/api/v1/conversations").param("q"," SEARCHCHATS ").param("before",firstPage.get("nextCursor").asText()).with(a)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(first)).andExpect(jsonPath("$.nextCursor").isEmpty());
  for(String query:java.util.List.of("%","_literal"))mvc.perform(get("/api/v1/conversations").param("q",query).with(a)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(first));
  mvc.perform(get("/api/v1/conversations").param("q","%' OR true --").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/conversations?q=SearchChats").with(oidcLogin().idToken(t->t.subject("conversation-search-outsider")))).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/conversations/"+first+"/messages/"+java.util.UUID.randomUUID()).with(a).with(csrf()).contentType("application/json").content("{\"text\":\"SecretContentNotSearchable\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/conversations?q=SecretContentNotSearchable").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(put("/api/v1/conversations/"+first+"/preferences").with(a).with(csrf()).contentType("application/json").content("{\"starred\":true,\"archived\":true,\"version\":0}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/conversations").param("q","100%").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  for(String view:java.util.List.of("STARRED","ARCHIVED"))mvc.perform(get("/api/v1/conversations").param("q","100%").param("view",view).with(a)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(post("/api/v1/blocks").with(a).with(csrf()).contentType("application/json").content("{\"memberId\":\""+firstMember+"\"}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/conversations").param("q","100%").param("view","ARCHIVED").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/conversations").param("q","x".repeat(121)).with(a)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/conversations?before=0&q=SearchChats").with(a)).andExpect(status().isBadRequest());
 }

 @Test @org.springframework.transaction.annotation.Transactional void networkDiscoveryRequiresConsentAndCurrentGraph()throws Exception{
  String viewer="graph-viewer",target="graph-target",middle="graph-middle";
  String viewerId=followProfile(viewer),targetId=followProfile(target),middleId=followProfile(middle);
  var v=oidcLogin().idToken(t->t.subject(viewer));var t=oidcLogin().idToken(token->token.subject(target));
  database.update("update profile set discoverable=true where subject in (?,?,?)",viewer,target,middle);
  database.update("insert into connection_request(id,sender,recipient,state) values(?,?,?,'ACCEPTED'),(?,?,?,'ACCEPTED')",java.util.UUID.randomUUID(),viewer,middle,java.util.UUID.randomUUID(),target,middle);
  mvc.perform(get("/api/v1/me/network-privacy").with(t)).andExpect(jsonPath("$.connectionsVisible").value(false));
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(status().isNotFound());
  mvc.perform(put("/api/v1/me/network-privacy").with(t).contentType("application/json").content("{\"connectionsVisible\":true,\"version\":0}")).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/me/network-privacy").with(t).with(csrf()).contentType("application/json").content("{\"connectionsVisible\":null,\"version\":0}")).andExpect(status().isBadRequest());
  mvc.perform(put("/api/v1/me/network-privacy").with(t).with(csrf()).contentType("application/json").content("{\"connectionsVisible\":true,\"version\":0}")).andExpect(jsonPath("$.version").value(1));
  mvc.perform(put("/api/v1/me/network-privacy").with(t).with(csrf()).contentType("application/json").content("{\"connectionsVisible\":false,\"version\":0}")).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(jsonPath("$.items.length()").value(0));
  database.update("update profile set connections_visible=true where subject=?",middle);
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(jsonPath("$.items[0].memberId").value(middleId));
  database.update("update profile set discoverable=false where subject=?",middle);
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(jsonPath("$.items.length()").value(0));
  database.update("update profile set discoverable=true where subject=?",middle);
  database.update("insert into member_block(blocker,blocked) values(?,?)",middle,viewer);
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(jsonPath("$.items.length()").value(0));
  database.update("delete from member_block where blocker=?",middle);
  database.update("update connection_request set state='REMOVED' where sender=?",target);
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(jsonPath("$.items.length()").value(0));
  database.update("insert into member_block(blocker,blocked) values(?,?)",target,viewer);
  mvc.perform(get("/api/v1/network/mutual/"+targetId).with(v)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/network/suggestions")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/network/mutual/"+viewerId).with(v)).andExpect(status().isNotFound());
 }
 @Test @org.springframework.transaction.annotation.Transactional void suggestionsExcludeActiveRelationshipsAndPrivateProfiles()throws Exception{
  String viewer="suggest-viewer",target="suggest-target";followProfile(viewer);followProfile(target);
  java.util.UUID id=java.util.UUID.fromString("00000000-0000-0000-0000-000000000012");database.update("update profile set public_id=?,discoverable=true,connections_visible=true where subject=?",id,target);
  var v=oidcLogin().idToken(t->t.subject(viewer));
  mvc.perform(get("/api/v1/network/suggestions").with(v)).andExpect(jsonPath("$.items[0].memberId").value(id.toString()));
  var invite=java.util.UUID.randomUUID();database.update("insert into connection_request(id,sender,recipient) values(?,?,?)",invite,viewer,target);
  String body=mvc.perform(get("/api/v1/network/suggestions").with(v)).andReturn().getResponse().getContentAsString();org.junit.jupiter.api.Assertions.assertFalse(body.contains(id.toString()));
  database.update("update connection_request set state='ACCEPTED' where id=?",invite);
  body=mvc.perform(get("/api/v1/network/suggestions").with(v)).andReturn().getResponse().getContentAsString();org.junit.jupiter.api.Assertions.assertFalse(body.contains(id.toString()));
  database.update("update connection_request set state='REMOVED' where id=?",invite);
  mvc.perform(get("/api/v1/network/suggestions").with(v)).andExpect(jsonPath("$.items[0].memberId").value(id.toString()));
  database.update("update profile set member_visible=false where subject=?",target);
  body=mvc.perform(get("/api/v1/network/suggestions").with(v)).andReturn().getResponse().getContentAsString();org.junit.jupiter.api.Assertions.assertFalse(body.contains(id.toString()));
 }
 @Test @org.springframework.transaction.annotation.Transactional void followersAreOwnerScopedRedactedAndPaged()throws Exception{
  String owner="followers-owner";followProfile(owner);var o=oidcLogin().idToken(t->t.subject(owner));
  for(int i=0;i<21;i++){String follower="follower-"+i;followProfile(follower);database.update("insert into member_follow(follower,followed) values(?,?)",follower,owner);}
  database.update("update profile set member_visible=false where subject='follower-20'");
  String body=mvc.perform(get("/api/v1/followers").with(o)).andExpect(jsonPath("$.total").value(21)).andExpect(jsonPath("$.items.length()").value(20)).andExpect(jsonPath("$.items[0].name").value("Private member")).andExpect(jsonPath("$.items[0].visible").value(false)).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
  mvc.perform(get("/api/v1/followers?before="+cursor).with(o)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty());
  mvc.perform(get("/api/v1/followers").with(oidcLogin().idToken(t->t.subject("follower-20")))).andExpect(jsonPath("$.total").value(0));
  database.update("insert into member_block(blocker,blocked) values(?,?)","follower-20",owner);
  mvc.perform(get("/api/v1/followers").with(o)).andExpect(jsonPath("$.total").value(20));
  mvc.perform(get("/api/v1/followers?before=0").with(o)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/v1/followers")).andExpect(status().isUnauthorized());
 }

 @Test void concurrentAcceptanceRetriesPublishOneOutboxEvent()throws Exception{
  String sender="retry-sender",recipient="retry-recipient";followProfile(sender);followProfile(recipient);
  var id=java.util.UUID.randomUUID();database.update("insert into connection_request(id,sender,recipient) values(?,?,?)",id,sender,recipient);
  try(var executor=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var start=new java.util.concurrent.CountDownLatch(1);
   java.util.concurrent.Callable<Integer> attempt=()->{start.await();return mvc.perform(put("/api/v1/network/"+id).with(oidcLogin().idToken(t->t.subject(recipient))).with(csrf()).contentType("application/json").content("{\"action\":\"ACCEPT\",\"version\":0}")).andReturn().getResponse().getStatus();};
   var a=executor.submit(attempt);var b=executor.submit(attempt);start.countDown();
   org.junit.jupiter.api.Assertions.assertEquals(204,a.get(15,java.util.concurrent.TimeUnit.SECONDS));org.junit.jupiter.api.Assertions.assertEquals(204,b.get(15,java.util.concurrent.TimeUnit.SECONDS));
  }
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from event_outbox where connection_id=?",Integer.class,id));
  org.junit.jupiter.api.Assertions.assertEquals(1L,database.queryForObject("select version from connection_request where id=?",Long.class,id));
 }

 @Test @org.springframework.transaction.annotation.Transactional void repliesAreBoundedIdempotentAndRespectParentVisibility()throws Exception{
  String owner="reply-owner",author="reply-parent",reader="reply-reader";for(String who:java.util.List.of(owner,author,reader))followProfile(who);
  var o=oidcLogin().idToken(t->t.subject(owner));var a=oidcLogin().idToken(t->t.subject(author));var r=oidcLogin().idToken(t->t.subject(reader));
  var post=java.util.UUID.randomUUID();var other=java.util.UUID.randomUUID();var parent=java.util.UUID.randomUUID();var reply=java.util.UUID.randomUUID();
  database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS'),(?,?,?,'MEMBERS')",post,owner,"Reply test",other,owner,"Other post");
  database.update("insert into post_comment(id,post_id,author,body) values(?,?,?,?)",parent,post,author,"Parent");
  String base="/api/v1/posts/"+post+"/comments",body=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("text","Reply body","parentId",parent.toString()));
  mvc.perform(put(base+"/"+reply).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(jsonPath("$.parentId").value(parent.toString()));
  mvc.perform(put(base+"/"+reply).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
  mvc.perform(put(base+"/"+reply).with(r).with(csrf()).contentType("application/json").content("{\"text\":\"Reply body\"}")).andExpect(status().isConflict());
  mvc.perform(get(base).with(r)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(parent.toString()));
  mvc.perform(get(base+"?parentId="+parent).with(r)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(put("/api/v1/posts/"+other+"/comments/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  mvc.perform(put(base+"/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body.replace(parent.toString(),reply.toString()))).andExpect(status().isNotFound());
  mvc.perform(delete(base+"/"+reply).with(a).with(csrf())).andExpect(status().isNotFound());
  database.update("insert into member_block(blocker,blocked) values(?,?)",author,reader);
  mvc.perform(get(base+"?parentId="+parent).with(r)).andExpect(status().isNotFound());
  mvc.perform(put(base+"/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  database.update("delete from member_block where blocker=?",author);
  database.update("update content_post set audience='ONLY_ME' where id=?",post);
  mvc.perform(get(base+"?parentId="+parent).with(r)).andExpect(status().isNotFound());
  database.update("update content_post set audience='MEMBERS' where id=?",post);
  mvc.perform(delete(base+"/"+parent).with(a).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(base+"?parentId="+parent).with(r)).andExpect(status().isNotFound());
  mvc.perform(put(base+"/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from post_comment where parent_id=?",Integer.class,parent));
 }
 @Test @org.springframework.transaction.annotation.Transactional void replyThreadsHaveIndependentCursorPagination()throws Exception{
  String owner="reply-page-owner";followProfile(owner);var user=oidcLogin().idToken(t->t.subject(owner));var post=java.util.UUID.randomUUID();var parent=java.util.UUID.randomUUID();
  database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS')",post,owner,"Thread pagination");
  database.update("insert into post_comment(id,post_id,author,body) values(?,?,?,?)",parent,post,owner,"Parent");
  for(int i=0;i<21;i++)database.update("insert into post_comment(id,post_id,author,body,parent_id) values(?,?,?,?,?)",java.util.UUID.randomUUID(),post,owner,"Reply "+i,parent);
  String url="/api/v1/posts/"+post+"/comments?parentId="+parent;
  String body=mvc.perform(get(url).with(user)).andExpect(jsonPath("$.items.length()").value(20)).andExpect(jsonPath("$.items[0].text").value("Reply 20")).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
  mvc.perform(get(url+"&before="+cursor).with(user)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].text").value("Reply 0")).andExpect(jsonPath("$.nextCursor").isEmpty());
 }

 @Test @org.springframework.transaction.annotation.Transactional void postDraftsArePrivateVersionedAndPublishOnce()throws Exception{
  followProfile("draft-owner");followProfile("draft-stranger");var owner=oidcLogin().idToken(t->t.subject("draft-owner"));var stranger=oidcLogin().idToken(t->t.subject("draft-stranger"));var id=java.util.UUID.randomUUID();String path="/api/v1/me/post-drafts/"+id;
  String initial="{\"text\":\"DraftOnlyNeedle\",\"audience\":\"MEMBERS\",\"version\":0}";
  mvc.perform(put(path).with(owner).contentType("application/json").content(initial)).andExpect(status().isForbidden());
  mvc.perform(put(path).with(owner).with(csrf()).contentType("application/json").content(initial)).andExpect(jsonPath("$.version").value(1));
  mvc.perform(put(path).with(owner).with(csrf()).contentType("application/json").content(initial)).andExpect(jsonPath("$.version").value(1));
  mvc.perform(get(path).with(stranger)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/me/post-drafts").with(stranger)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/search/posts?q=DraftOnlyNeedle").with(stranger)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(post(path+"/publish").with(stranger).with(csrf()).contentType("application/json").content("{\"version\":1}")).andExpect(status().isNotFound());
  String update="{\"text\":\"DraftOnlyNeedle updated\",\"audience\":\"MEMBERS\",\"version\":1}";
  mvc.perform(put(path).with(owner).with(csrf()).contentType("application/json").content(update)).andExpect(jsonPath("$.version").value(2));
  mvc.perform(put(path).with(owner).with(csrf()).contentType("application/json").content(update)).andExpect(status().isConflict());
  mvc.perform(post(path+"/publish").with(owner).with(csrf()).contentType("application/json").content("{\"version\":1}")).andExpect(status().isConflict());
  for(int n=0;n<2;n++)mvc.perform(post(path+"/publish").with(owner).with(csrf()).contentType("application/json").content("{\"version\":2}")).andExpect(jsonPath("$.id").value(id.toString()));
  mvc.perform(get("/api/v1/me/post-drafts").with(owner)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/search/posts?q=DraftOnlyNeedle").with(stranger)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(delete(path+"?version=3").with(owner).with(csrf())).andExpect(status().isConflict());
  mvc.perform(delete("/api/v1/posts/"+id+"?version=1").with(owner).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(post(path+"/publish").with(owner).with(csrf()).contentType("application/json").content("{\"version\":2}")).andExpect(status().isNotFound());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from content_post where id=?",Integer.class,id));
 }
 @Test @org.springframework.transaction.annotation.Transactional void draftCapacityPaginationAndBlankPublicationAreEnforced()throws Exception{
  String subject="draft-capacity";followProfile(subject);var user=oidcLogin().idToken(t->t.subject(subject));
  for(int n=0;n<100;n++)database.update("insert into post_draft(id,author,body,audience) values(?,?,?,'ONLY_ME')",java.util.UUID.randomUUID(),subject,"");
  var id=database.queryForObject("select id from post_draft where author=? order by id limit 1",java.util.UUID.class,subject);
  mvc.perform(post("/api/v1/me/post-drafts/"+id+"/publish").with(user).with(csrf()).contentType("application/json").content("{\"version\":1}")).andExpect(status().isConflict());
  mvc.perform(put("/api/v1/me/post-drafts/"+java.util.UUID.randomUUID()).with(user).with(csrf()).contentType("application/json").content("{\"text\":\"extra\",\"audience\":\"ONLY_ME\",\"version\":0}")).andExpect(status().isConflict());
  String body=mvc.perform(get("/api/v1/me/post-drafts").with(user)).andExpect(jsonPath("$.items.length()").value(20)).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();
  mvc.perform(get("/api/v1/me/post-drafts?after="+cursor).with(user)).andExpect(jsonPath("$.items.length()").value(20));
  mvc.perform(delete("/api/v1/me/post-drafts/"+id+"?version=1").with(user).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/me/post-drafts/"+id).with(user)).andExpect(status().isNotFound());
 }
 @Test void concurrentDraftPublicationDoesNotDuplicatePosts()throws Exception{
  String subject="draft-race";followProfile(subject);var id=java.util.UUID.randomUUID();database.update("insert into post_draft(id,author,body,audience) values(?,?,?,'ONLY_ME')",id,subject,"Concurrent draft");
  try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.Callable<Integer> publish=()->{gate.await();return mvc.perform(post("/api/v1/me/post-drafts/"+id+"/publish").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("application/json").content("{\"version\":1}")).andReturn().getResponse().getStatus();};
   var a=pool.submit(publish);var b=pool.submit(publish);gate.countDown();org.junit.jupiter.api.Assertions.assertEquals(200,a.get(15,java.util.concurrent.TimeUnit.SECONDS));org.junit.jupiter.api.Assertions.assertEquals(200,b.get(15,java.util.concurrent.TimeUnit.SECONDS));
  }
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from content_post where id=?",Integer.class,id));
 }

 @Test @org.springframework.transaction.annotation.Transactional void activityEnforcesPostPrivacyAndStableAuthorPagination()throws Exception{
  String author="activity-author",reader="activity-reader";String id=followProfile(author);followProfile(reader);var a=oidcLogin().idToken(t->t.subject(author));var r=oidcLogin().idToken(t->t.subject(reader));
  for(int n=0;n<21;n++)database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS')",java.util.UUID.randomUUID(),author,"Activity "+n);
  database.update("insert into content_post(id,author,body,audience) values(?,?,?,'ONLY_ME')",java.util.UUID.randomUUID(),author,"Private activity");
  database.update("insert into content_post(id,author,body,audience,deleted_at) values(?,?,?,'MEMBERS',now())",java.util.UUID.randomUUID(),author,"Deleted activity");
  String url="/api/v1/activity/"+id;
  mvc.perform(get(url)).andExpect(status().isUnauthorized());
  mvc.perform(get(url+"?before=0").with(r)).andExpect(status().isBadRequest());
  String body=mvc.perform(get(url).with(r)).andExpect(jsonPath("$.items.length()").value(20)).andExpect(jsonPath("$.items[0].text").value("Activity 20")).andReturn().getResponse().getContentAsString();
  String cursor=new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).get("nextCursor").asText();mvc.perform(get(url+"?before="+cursor).with(r)).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].text").value("Activity 0"));
  mvc.perform(get(url).with(a)).andExpect(jsonPath("$.items[0].text").value("Private activity"));
  database.update("update profile set member_visible=false where subject=?",author);
  mvc.perform(get(url).with(r)).andExpect(jsonPath("$.items.length()").value(20));
  database.update("insert into member_block(blocker,blocked) values(?,?)",author,reader);
  mvc.perform(get(url).with(r)).andExpect(jsonPath("$.items.length()").value(0));
 }

 @Test @org.springframework.transaction.annotation.Transactional void moderationIsPrivilegedAuditedAndEnforcedAcrossReaders()throws Exception{
  String author="moderated-author",reporter="moderated-reporter",reviewer="moderated-reviewer";String authorId=followProfile(author);followProfile(reporter);followProfile(reviewer);
  var a=oidcLogin().idToken(t->t.subject(author));var r=oidcLogin().idToken(t->t.subject(reporter));var m=oidcLogin().idToken(t->t.subject(reviewer));
  var post=java.util.UUID.randomUUID();var id=java.util.UUID.randomUUID();database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS')",post,author,"ModerationNeedle");
  String report="/api/v1/reports/"+id,review="/api/v1/moderation/reports/"+id,body="{\"postId\":\""+post+"\",\"category\":\"SPAM\",\"details\":\"Fictional report\"}";
  mvc.perform(put(report).with(r).contentType("application/json").content(body)).andExpect(status().isForbidden());
  for(int n=0;n<2;n++)mvc.perform(put(report).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(jsonPath("$.state").value("OPEN"));
  mvc.perform(put("/api/v1/reports/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/reports").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/moderation/reports").with(r)).andExpect(status().isForbidden());
  database.update("insert into moderation_operator(subject) values(?),(?),(?)",author,reporter,reviewer);
  String decision="{\"decision\":\"REMOVED\",\"reason\":\"Fictional policy violation\",\"version\":1,\"postVersion\":1}";
  for(var actor:java.util.List.of(a,r))mvc.perform(put(review).with(actor).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/moderation/reports").with(m)).andExpect(jsonPath("$.items[0].postText").value("ModerationNeedle")).andExpect(jsonPath("$.items[0].reporter").doesNotExist());
  database.update("update content_post set version=version+1,body='ModerationNeedle revised' where id=?",post);
  mvc.perform(put(review).with(m).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isConflict());
  org.junit.jupiter.api.Assertions.assertEquals(0,database.queryForObject("select count(*) from moderation_audit where report_id=?",Integer.class,id));
  decision=decision.replace("\"postVersion\":1","\"postVersion\":2");
  for(int n=0;n<2;n++)mvc.perform(put(review).with(m).with(csrf()).contentType("application/json").content(decision)).andExpect(jsonPath("$.state").value("REMOVED"));
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from moderation_audit where report_id=?",Integer.class,id));
  mvc.perform(put(review).with(m).with(csrf()).contentType("application/json").content(decision.replace("REMOVED","DISMISSED"))).andExpect(status().isConflict());
  for(var viewer:java.util.List.of(a,r,m)){
   mvc.perform(get("/api/v1/posts/"+post).with(viewer)).andExpect(status().isNotFound());
   mvc.perform(get("/api/v1/posts/"+post+"/comments").with(viewer)).andExpect(status().isNotFound());
   mvc.perform(get("/api/v1/search/posts?q=ModerationNeedle").with(viewer)).andExpect(jsonPath("$.items.length()").value(0));
   mvc.perform(get("/api/v1/activity/"+authorId).with(viewer)).andExpect(jsonPath("$.items.length()").value(0));
  }
  mvc.perform(get("/api/v1/reports").with(r)).andExpect(jsonPath("$.items[0].state").value("REMOVED")).andExpect(jsonPath("$.items[0].reason").doesNotExist());
  database.update("delete from moderation_operator where subject=?",reviewer);
  mvc.perform(put(review).with(m).with(csrf()).contentType("application/json").content(decision)).andExpect(status().isForbidden());
 }
 @Test @org.springframework.transaction.annotation.Transactional void reportsRequireVisibilityAndRespectRateLimit()throws Exception{
  String author="report-limit-author",reporter="report-limit-member";followProfile(author);followProfile(reporter);var r=oidcLogin().idToken(t->t.subject(reporter));var post=java.util.UUID.randomUUID();database.update("insert into content_post(id,author,body,audience) values(?,?,?,'ONLY_ME')",post,author,"Private report target");
  String body="{\"postId\":\""+post+"\",\"category\":\"OTHER\",\"details\":\"\"}";
  mvc.perform(put("/api/v1/reports/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isNotFound());
  database.update("update content_post set audience='MEMBERS' where id=?",post);
  for(int n=0;n<20;n++){var target=java.util.UUID.randomUUID();database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS')",target,author,"Report fixture");database.update("insert into content_report(id,reporter,post_id,category,details) values(?,?,?,'OTHER','')",java.util.UUID.randomUUID(),reporter,target);}
  mvc.perform(put("/api/v1/reports/"+java.util.UUID.randomUUID()).with(r).with(csrf()).contentType("application/json").content(body)).andExpect(status().isTooManyRequests());
  mvc.perform(get("/api/v1/reports")).andExpect(status().isUnauthorized());
 }

 @Test @org.springframework.transaction.annotation.Transactional void hiddenPostsArePrivateIdempotentAndNeverBypassVisibility()throws Exception{
  String author="hidden-author",reader="hidden-reader";String id=followProfile(author);followProfile(reader);var a=oidcLogin().idToken(t->t.subject(author));var r=oidcLogin().idToken(t->t.subject(reader));var post=java.util.UUID.randomUUID();database.update("insert into content_post(id,author,body,audience) values(?,?,?,'MEMBERS')",post,author,"HideNeedle");
  database.update("insert into member_follow(follower,followed) values(?,?)",reader,author);database.update("insert into saved_post(post_id,member) values(?,?)",post,reader);
  String url="/api/v1/posts/"+post+"/hidden";
  mvc.perform(put(url).with(r).contentType("application/json").content("{\"hidden\":true}")).andExpect(status().isForbidden());
  mvc.perform(put(url).with(r).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
  for(int n=0;n<2;n++)mvc.perform(put(url).with(r).with(csrf()).contentType("application/json").content("{\"hidden\":true}")).andExpect(status().isNoContent());
  org.junit.jupiter.api.Assertions.assertEquals(1,database.queryForObject("select count(*) from hidden_post where member=? and post_id=?",Integer.class,reader,post));
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/feed?scope=HIDDEN").with(r)).andExpect(jsonPath("$.items[0].id").value(post.toString()));
  mvc.perform(get("/api/v1/feed?scope=HIDDEN").with(a)).andExpect(jsonPath("$.items.length()").value(0));
  mvc.perform(get("/api/v1/saved-posts").with(r)).andExpect(jsonPath("$.items[0].id").value(post.toString()));
  mvc.perform(get("/api/v1/search/posts?q=HideNeedle").with(r)).andExpect(jsonPath("$.items.length()").value(1));
  for(int n=0;n<2;n++)mvc.perform(put(url).with(r).with(csrf()).contentType("application/json").content("{\"hidden\":false}")).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/feed?scope=FOLLOWING").with(r)).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(put(url).with(r).with(csrf()).contentType("application/json").content("{\"hidden\":true}")).andExpect(status().isNoContent());
  database.update("update content_post set audience='ONLY_ME' where id=?",post);mvc.perform(get("/api/v1/feed?scope=HIDDEN").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  database.update("update content_post set audience='MEMBERS',moderated_at=now() where id=?",post);mvc.perform(get("/api/v1/feed?scope=HIDDEN").with(r)).andExpect(jsonPath("$.items.length()").value(0));
  database.update("update content_post set moderated_at=null,deleted_at=now() where id=?",post);mvc.perform(get("/api/v1/feed?scope=HIDDEN").with(r)).andExpect(jsonPath("$.items.length()").value(0));
 }

 static String encodeFixture(Object value){try{return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
 NotificationProjector projectionFixture(org.springframework.context.ApplicationEventPublisher publisher){return new NotificationProjector(new com.meridian.notifications.application.NotificationProjectionService(new com.meridian.notifications.adapter.JdbcNotificationProjectionStore(database),new com.meridian.notifications.adapter.NotificationRealtimeAdapter(publisher),networkTransactions));}
}
