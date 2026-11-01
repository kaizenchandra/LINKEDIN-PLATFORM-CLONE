package com.meridian;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** Private immutable normalized images. URLs and storage credentials never reach the browser. */
interface PhotoObjects {
 final class Missing extends RuntimeException{}
 void put(UUID id,byte[] photo);
 byte[] get(UUID id);
 void delete(UUID id);
}
@Configuration
@ConditionalOnProperty(name="meridian.media.s3.enabled",havingValue="true")
class PhotoObjectConfiguration {
 @Bean(destroyMethod="close") S3Client photoS3(@Value("${meridian.media.s3.region:us-east-1}") String region,@Value("${meridian.media.s3.endpoint:}") String endpoint){
  var builder=S3Client.builder().region(Region.of(region)).overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(8)).apiCallAttemptTimeout(Duration.ofSeconds(3)));
  if(!endpoint.isBlank())builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
  return builder.build();
 }
 @Bean(destroyMethod="close") PhotoCleanup photoCleanup(ProfilePhotos photos){return new PhotoCleanup(photos);}
 @Bean PhotoObjects photoObjects(S3Client client,@Value("${meridian.media.s3.bucket}") String bucket){return new PhotoObjects(){
  private String key(UUID id){return "profiles/"+id+".png";}
  public void put(UUID id,byte[] photo){client.putObject(r->r.bucket(bucket).key(key(id)).contentType("image/png").serverSideEncryption(ServerSideEncryption.AES256),RequestBody.fromBytes(photo));}
  public byte[] get(UUID id){
   try(var body=client.getObject(r->r.bucket(bucket).key(key(id)))){
    byte[] bytes=body.readNBytes(ProfilePhotoController.MAX_BYTES+1);
    if(bytes.length>ProfilePhotoController.MAX_BYTES)throw new IllegalStateException("Stored image exceeds limit");
    return bytes;
   }catch(software.amazon.awssdk.services.s3.model.NoSuchKeyException missing){throw new PhotoObjects.Missing();}
   catch(IOException failure){throw new IllegalStateException("Media read unavailable");}
  }
  public void delete(UUID id){client.deleteObject(r->r.bucket(bucket).key(key(id)));}
 };}
}

/** Isolated from Spring's event/socket scheduler; one bounded cleanup batch at a time. */
final class PhotoCleanup implements AutoCloseable {
 private final java.util.concurrent.ScheduledExecutorService executor=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("media-cleanup").factory());
 PhotoCleanup(ProfilePhotos photos){executor.scheduleWithFixedDelay(photos::cleanup,60,60,java.util.concurrent.TimeUnit.SECONDS);}
 public void close(){
  executor.shutdown();
  try{if(!executor.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS))executor.shutdownNow();}
  catch(InterruptedException interrupted){executor.shutdownNow();Thread.currentThread().interrupt();}
 }
}
