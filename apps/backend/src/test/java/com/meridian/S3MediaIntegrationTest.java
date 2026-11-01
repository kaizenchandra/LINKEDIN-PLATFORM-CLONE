package com.meridian;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.*;
import software.amazon.awssdk.services.s3.S3Client;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"meridian.exports.worker-enabled=false","meridian.events.enabled=false","meridian.media.s3.enabled=true","meridian.media.s3.bucket=meridian-test-media","spring.security.oauth2.client.provider.keycloak.issuer-uri=", "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost/authorize", "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost/token", "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost/jwks", "spring.security.oauth2.client.registration.keycloak.client-secret=test"})
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @Testcontainers
class S3MediaIntegrationTest {
 @org.springframework.test.context.bean.override.mockito.MockitoBean org.springframework.security.oauth2.client.registration.ClientRegistrationRepository registrations;
 @org.springframework.test.context.bean.override.mockito.MockitoSpyBean PhotoObjects objects;
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17");
 @Container static GenericContainer<?> localstack=new GenericContainer<>("localstack/localstack:4.10.0").withEnv("SERVICES","s3").withExposedPorts(4566).waitingFor(Wait.forLogMessage(".*Ready\\..*",1).withStartupTimeout(Duration.ofMinutes(2)));
 @DynamicPropertySource static void config(DynamicPropertyRegistry r){
  r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);
  r.add("meridian.media.s3.endpoint",()->"http://"+localstack.getHost()+":"+localstack.getMappedPort(4566));
  System.setProperty("aws.accessKeyId","test");System.setProperty("aws.secretAccessKey","test");
 }
 @Autowired MockMvc mvc;@Autowired JdbcTemplate db;@Autowired S3Client s3;@Autowired ProfilePhotos photos;@Autowired AccountDataSnapshot exports;
 @Autowired org.springframework.context.ApplicationContext context;@Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @BeforeEach void bucket(){s3.createBucket(r->r.bucket("meridian-test-media"));}
 @AfterEach void resetSpy(){reset(objects);}
 @AfterAll static void clearCredentials(){System.clearProperty("aws.accessKeyId");System.clearProperty("aws.secretAccessKey");}
 byte[] png()throws Exception{var buffer=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",buffer);return buffer.toByteArray();}
 void member(String subject)throws Exception{mvc.perform(get("/api/v1/me").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isOk());}
 UUID objectId(String subject){return db.queryForObject("select photo_object_id from profile where subject=?",UUID.class,subject);}
 void upload(String subject)throws Exception{mvc.perform(put("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("image/png").content(png())).andExpect(status().isNoContent());}
 @Test void normalizedPhotoUsesS3WhilePrivacyAndExportsRemainAuthoritative()throws Exception{
  String subject="s3-private";member(subject);upload(subject);UUID id=objectId(subject);
  assertNotNull(id);assertNull(db.queryForObject("select photo from profile where subject=?",byte[].class,subject));
  assertEquals("image/png",s3.headObject(r->r.bucket("meridian-test-media").key("profiles/"+id+".png")).contentType());
  UUID publicId=db.queryForObject("select public_id from profile where subject=?",UUID.class,subject);
  mvc.perform(get("/api/v1/profiles/"+publicId+"/photo")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
  String export=new String(exports.generate(subject),java.nio.charset.StandardCharsets.UTF_8);assertTrue(export.contains("photoPngBase64"));assertFalse(export.contains(id.toString()));assertFalse(export.contains("photo_object_id"));
  db.update("update profile set public_visible=true where subject=?",subject);
  mvc.perform(get("/api/v1/profiles/"+publicId+"/photo")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
 }
 @Test void failedStorageWriteKeepsLastPhotoAndFailedReadReturns503()throws Exception{
  String subject="s3-failure";member(subject);upload(subject);UUID before=objectId(subject);
  doThrow(new IllegalStateException("test provider failure")).when(objects).put(any(UUID.class),any(byte[].class));
  mvc.perform(put("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf()).contentType("image/png").content(png())).andExpect(status().isServiceUnavailable());assertEquals(before,objectId(subject));
  doThrow(new IllegalStateException("test read failure")).when(objects).get(before);
  mvc.perform(get("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isServiceUnavailable());
 }
 @Test void replacementAndRemovalCollectOnlyUnreferencedObjectsAndRetryFailures()throws Exception{
  String subject="s3-gc";member(subject);upload(subject);UUID old=objectId(subject);upload(subject);UUID current=objectId(subject);
  db.update("update profile_media_object set created_at=now()-interval '20 minutes' where id in (?,?)",old,current);
  doThrow(new IllegalStateException("test delete failure")).when(objects).delete(old);photos.cleanup();
  assertEquals(1,db.queryForObject("select count(*) from profile_media_object where id=?",Integer.class,old));reset(objects);photos.cleanup();
  assertEquals(0,db.queryForObject("select count(*) from profile_media_object where id=?",Integer.class,old));assertNotNull(objects.get(current));
  mvc.perform(delete("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject))).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isNotFound());photos.cleanup();
  assertThrows(PhotoObjects.Missing.class,()->objects.get(current));
 }
 @Test void localRecoveryRestoresMissingObjectsButDoesNotResurrectCollectedMedia()throws Exception{
  String subject="s3-repair";member(subject);upload(subject);UUID id=objectId(subject);byte[] original=objects.get(id);
  db.update("update profile set photo=? where subject=?",original,subject);
  var local=new ProfilePhotos(db,context.getBeanProvider(PhotoObjects.class),transactions,new io.micrometer.core.instrument.simple.SimpleMeterRegistry(),true);
  objects.delete(id);assertArrayEquals(original,local.owner(subject));assertArrayEquals(original,objects.get(id));
  local.remove(subject);db.update("update profile_media_object set created_at=now()-interval '20 minutes' where id=?",id);local.cleanup();
  assertArrayEquals(original,local.read(new ProfilePhotos.Photo(original,id)));
  assertThrows(PhotoObjects.Missing.class,()->objects.get(id));
 }
 @Test void existingDatabasePhotoRemainsReadableUntilReplaced()throws Exception{
  String subject="s3-legacy";member(subject);db.update("update profile set photo=? where subject=?",png(),subject);
  mvc.perform(get("/api/v1/me/photo").with(oidcLogin().idToken(t->t.subject(subject)))).andExpect(status().isOk());upload(subject);assertNotNull(objectId(subject));
 }
}
