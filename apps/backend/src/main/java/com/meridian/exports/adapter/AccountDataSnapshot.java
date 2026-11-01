package com.meridian.exports.adapter;
import com.meridian.media.application.ProfilePhotoUseCases;
import com.meridian.exports.application.ExportSnapshot;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Explicit owner-scoped projections: never serialize sessions or identity-provider stores. */
@Service
public class AccountDataSnapshot implements ExportSnapshot {
 static final int MAX_BYTES=10*1024*1024,MAX_ROWS=10000;
 private final JdbcTemplate db;private final ObjectMapper json;private final ProfilePhotoUseCases photos;
 public AccountDataSnapshot(JdbcTemplate db,ObjectMapper json,ProfilePhotoUseCases photos){this.db=db;this.json=json;this.photos=photos;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ,timeout=30)
 public byte[] generate(String subject){
  var buffer=new ByteArrayOutputStream();
  OutputStream bounded=new OutputStream(){
   public void write(int value){if(buffer.size()>=MAX_BYTES)throw new TooLarge();buffer.write(value);}
   public void write(byte[] data,int offset,int length){if(length>MAX_BYTES-buffer.size())throw new TooLarge();buffer.write(data,offset,length);}
  };
  try(var out=json.getFactory().createGenerator(bounded)){
   out.writeStartObject();out.writeNumberField("schemaVersion",1);out.writeStringField("generatedAt",Instant.now().toString());
   out.writeStringField("scope","Your Meridian business data, including retained authored content and sent messages. Identity-provider records and other members' private contributions are excluded.");
   for(var section:queries().entrySet()){
    out.writeArrayFieldStart(section.getKey());int[] count={0};
    String query=section.getValue();Object[] args=Collections.nCopies((int)query.chars().filter(c->c=='?').count(),subject).toArray();
    db.query(connection->{
     var statement=connection.prepareStatement(query+" limit "+(MAX_ROWS+1));statement.setFetchSize(100);
     for(int i=0;i<args.length;i++)statement.setString(i+1,(String)args[i]);return statement;
    },(RowCallbackHandler)row->{
     if(++count[0]>MAX_ROWS)throw new TooLarge();
     try{
      if(section.getKey().equals("profile")){
       var profile=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(row.getString(1));
       byte[] photo=photos.owner(subject);if(photo==null)profile.putNull("photoPngBase64");else profile.put("photoPngBase64",Base64.getEncoder().encodeToString(photo));
       out.writeTree(profile);
      }else out.writeRawValue(row.getString(1));
     }catch(IOException failure){throw new UncheckedIOException(failure);}
    });
    out.writeEndArray();
   }
   out.writeEndObject();out.flush();return buffer.toByteArray();
  }catch(IOException failure){throw new UncheckedIOException(failure);}
 }
 private Map<String,String> queries(){
  var queries=new LinkedHashMap<String,String>();
  queries.put("profile","select to_jsonb(p)-'subject'-'photo'-'photo_object_id' from profile p where subject=?");
  for(String table:List.of("experience","education","certifications","projects","profile_skill","profile_language","account_preferences","notification_preference","job_preference"))
   queries.put(table,"select to_jsonb(t)-'subject'-'name_key' from "+table+" t where subject=?");
  queries.put("searchHistoryPreference","select to_jsonb(t)-'subject' from search_history_preference t where subject=?");
  queries.put("searchHistory","select to_jsonb(t)-'subject'-'query_key'-'location_key' from search_history t where subject=? and searched_at>now()-interval '30 days'");
  queries.put("connections","select jsonb_build_object('id',c.id,'state',c.state,'version',c.version,'createdAt',c.created_at,'sentByYou',c.sender=?,'memberId',p.public_id) from connection_request c join profile p on p.subject=case when c.sender=? then c.recipient else c.sender end where ? in(c.sender,c.recipient)");
  queries.put("following","select jsonb_build_object('memberId',p.public_id,'createdAt',f.created_at) from member_follow f join profile p on p.subject=f.followed where f.follower=?");
  queries.put("blocks","select jsonb_build_object('memberId',p.public_id,'createdAt',b.created_at) from member_block b join profile p on p.subject=b.blocked where b.blocker=?");
  queries.put("posts","select to_jsonb(t)-'author' from content_post t where author=?");
  queries.put("postDrafts","select to_jsonb(t)-'author' from post_draft t where author=?");
  queries.put("comments","select to_jsonb(t)-'author' from post_comment t where author=?");
  queries.put("reactions","select to_jsonb(t)-'member' from post_reaction t where member=?");
  queries.put("hiddenPosts","select to_jsonb(t)-'member' from hidden_post t where member=?");
  queries.put("savedPosts","select to_jsonb(t)-'member' from saved_post t where member=?");
  queries.put("sentMessages","select to_jsonb(t)-'sender' from direct_message t where sender=?");
  queries.put("conversations","select jsonb_build_object('id',c.id,'createdAt',c.created_at,'memberId',p.public_id,'readThrough',case when c.member_a=? then c.read_a else c.read_b end,'starred',case when c.member_a=? then c.starred_a else c.starred_b end,'archived',case when c.member_a=? then c.archived_a else c.archived_b end) from direct_conversation c join profile p on p.subject=case when c.member_a=? then c.member_b else c.member_a end where ? in(c.member_a,c.member_b)");
  queries.put("organizations","select to_jsonb(t)-'owner_subject' from organization t where owner_subject=?");
  queries.put("managedJobPosts","select to_jsonb(j) from job j join organization o on o.id=j.organization_id where o.owner_subject=?");
  queries.put("managedJobQuestions","select to_jsonb(q) from job_question_form q join job j on j.id=q.job_id join organization o on o.id=j.organization_id where o.owner_subject=?");
  queries.put("recruiterAccess","select to_jsonb(t)-'member' from recruiter_access t where member=?");
  queries.put("applications","select (to_jsonb(a)-'applicant')||jsonb_build_object('resumeText',r.body) from job_application a left join application_resume r on r.application_id=a.id where a.applicant=?");
  queries.put("applicationHistory","select to_jsonb(h)-'actor' from application_audit h join job_application a on a.id=h.application_id where a.applicant=?");
  for(String table:List.of("saved_job","dismissed_job","organization_follow"))queries.put(table,"select to_jsonb(t)-'subject' from "+table+" t where subject=?");
  queries.put("submittedReports","select to_jsonb(t)-'reporter' from content_report t where reporter=?");
  queries.put("notifications","select to_jsonb(t)-'recipient'-'actor' from notification t where recipient=?");
  queries.put("sessionRevocations","select to_jsonb(t)-'subject' from session_revocation_audit t where subject=?");
  return queries;
 }
}
