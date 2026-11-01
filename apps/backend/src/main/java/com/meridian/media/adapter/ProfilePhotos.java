package com.meridian.media.adapter;
import com.meridian.media.application.ProfilePhotoStore;
import com.meridian.media.application.ProfilePhotoStore.Photo;
import com.meridian.media.application.PhotoObjects;
import com.meridian.media.application.PhotoMaintenance;

import java.util.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfilePhotos implements PhotoMaintenance, ProfilePhotoStore {
 private final JdbcTemplate db;private final PhotoObjects objects;private final TransactionTemplate transaction;private final MeterRegistry metrics;private final boolean retainDbCopy;private final TransactionTemplate repairTransaction;
 public ProfilePhotos(JdbcTemplate db,ObjectProvider<PhotoObjects> objects,PlatformTransactionManager manager,MeterRegistry metrics,@org.springframework.beans.factory.annotation.Value("${meridian.media.s3.retain-db-copy:false}") boolean retainDbCopy){this.db=db;this.objects=objects.getIfAvailable();this.transaction=new TransactionTemplate(manager);this.metrics=metrics;this.retainDbCopy=retainDbCopy;this.repairTransaction=new TransactionTemplate(manager);this.repairTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
 public void save(String subject,byte[] bytes){
  if(objects==null){
   if(db.update("update profile set photo=?,photo_object_id=null where subject=?",bytes,subject)!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Initialize profile first");return;
  }
  if(!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from profile where subject=?)",Boolean.class,subject)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Initialize profile first");
  UUID id=UUID.randomUUID();
  // Commit the cleanup intent before contacting storage, including uncertain PUT outcomes.
  db.update("insert into profile_media_object(id) values(?)",id);
  try{objects.put(id,bytes);}catch(RuntimeException failure){throw unavailable();}
  transaction.executeWithoutResult(status->{
   if(db.query("select id from profile_media_object where id=? for update",(r,n)->r.getObject(1,UUID.class),id).isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Upload expired; please retry");
   if(db.update("update profile set photo=?,photo_object_id=? where subject=?",retainDbCopy?bytes:null,id,subject)!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"Initialize profile first");
  });
  metrics.counter("meridian.media.uploads").increment();
 }
 public void remove(String subject){db.update("update profile set photo=null,photo_object_id=null where subject=?",subject);}
 public byte[] read(Photo photo){
  if(photo.objectId()==null)return photo.legacy();
  if(objects==null)throw unavailable();
  try{return objects.get(photo.objectId());}
  catch(PhotoObjects.Missing missing){
   if(retainDbCopy&&photo.legacy()!=null){
    try{return repairTransaction.execute(status->{
     // Serialize repair with collection; never recreate an object whose cleanup intent is gone.
     var live=db.query("select id from profile_media_object where id=? for update",(r,n)->r.getObject(1,UUID.class),photo.objectId());
     if(!live.isEmpty()){objects.put(photo.objectId(),photo.legacy());metrics.counter("meridian.media.local.repairs").increment();}
     return photo.legacy();
    });}
    catch(RuntimeException failure){throw unavailable();}
   }
   throw unavailable();
  }catch(RuntimeException failure){throw unavailable();}
 }
 Photo row(java.sql.ResultSet row)throws java.sql.SQLException{return new Photo(row.getBytes("photo"),row.getObject("photo_object_id",UUID.class));}
 public byte[] owner(String subject){var rows=db.query("select photo,photo_object_id from profile where subject=?",(r,n)->row(r),subject);return rows.isEmpty()?null:read(rows.getFirst());}
 public Optional<Photo> visible(String subject,UUID id){return db.query("select photo,photo_object_id from profile p where public_id=? and (public_visible or (cast(? as text) is not null and member_visible) or subject=?) and not exists(select 1 from member_block b where (b.blocker=? and b.blocked=p.subject) or (b.blocked=? and b.blocker=p.subject))",(r,n)->row(r),id,subject,subject,subject,subject).stream().findFirst();}
 private ResponseStatusException unavailable(){metrics.counter("meridian.media.failures").increment();return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Photo storage is temporarily unavailable; retry later");}
 public void cleanup(){
  if(objects==null)return;
  for(int i=0;i<10;i++){
   try{
    boolean removed=Boolean.TRUE.equals(transaction.execute(status->{
     var candidates=db.query("select id from profile_media_object m where created_at<now()-interval '15 minutes' and not exists(select 1 from profile p where p.photo_object_id=m.id) order by created_at limit 1 for update skip locked",(r,n)->r.getObject(1,UUID.class));
     if(candidates.isEmpty())return false;
     UUID id=candidates.getFirst();objects.delete(id);db.update("delete from profile_media_object where id=?",id);return true;
    }));
    if(!removed)return;metrics.counter("meridian.media.collected").increment();
   }catch(RuntimeException failure){metrics.counter("meridian.media.cleanup.failures").increment();return;}
  }
 }
}
