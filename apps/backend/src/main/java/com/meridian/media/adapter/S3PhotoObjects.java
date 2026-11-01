package com.meridian.media.adapter;
import java.io.IOException;
import java.util.UUID;
import com.meridian.media.application.PhotoObjects;
import com.meridian.media.domain.PhotoLimits;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
public final class S3PhotoObjects implements PhotoObjects {
 private final S3Client client;private final String bucket;
 public S3PhotoObjects(S3Client client,String bucket){this.client=client;this.bucket=bucket;}
  private String key(UUID id){return "profiles/"+id+".png";}
  public void put(UUID id,byte[] photo){client.putObject(r->r.bucket(bucket).key(key(id)).contentType("image/png").serverSideEncryption(ServerSideEncryption.AES256),RequestBody.fromBytes(photo));}
  public byte[] get(UUID id){
   try(var body=client.getObject(r->r.bucket(bucket).key(key(id)))){
    byte[] bytes=body.readNBytes(PhotoLimits.MAX_BYTES+1);
    if(bytes.length>PhotoLimits.MAX_BYTES)throw new IllegalStateException("Stored image exceeds limit");
    return bytes;
   }catch(software.amazon.awssdk.services.s3.model.NoSuchKeyException missing){throw new PhotoObjects.Missing();}
   catch(IOException failure){throw new IllegalStateException("Media read unavailable");}
  }
  public void delete(UUID id){client.deleteObject(r->r.bucket(bucket).key(key(id)));}
}
