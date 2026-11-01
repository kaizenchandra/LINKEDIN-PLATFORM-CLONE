package com.meridian.media.adapter;
import java.net.URI;
import java.time.Duration;
import com.meridian.media.application.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.regions.Region;
@Configuration @ConditionalOnProperty(name="meridian.media.s3.enabled",havingValue="true")
public class PhotoObjectConfiguration {
 @Bean(destroyMethod="close") S3Client photoS3(@Value("${meridian.media.s3.region:us-east-1}") String region,@Value("${meridian.media.s3.endpoint:}") String endpoint){
  var builder=S3Client.builder().region(Region.of(region)).overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(8)).apiCallAttemptTimeout(Duration.ofSeconds(3)));
  if(!endpoint.isBlank())builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
  return builder.build();
 }
 @Bean(destroyMethod="close") PhotoCleanup photoCleanup(PhotoMaintenance photos){return new PhotoCleanup(photos);}
 @Bean PhotoObjects photoObjects(S3Client client,@Value("${meridian.media.s3.bucket}") String bucket){return new S3PhotoObjects(client,bucket);}
}
