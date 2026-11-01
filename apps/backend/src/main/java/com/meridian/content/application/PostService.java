package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class PostService implements PostUseCases {
 private final PostStore store;private final ContentAccess access;private final Transactions transactions;
 public PostService(PostStore store,ContentAccess access,Transactions transactions){this.store=store;this.access=access;this.transactions=transactions;}
 private UseCaseFailure conflict(){return new UseCaseFailure(CONFLICT,"Post unavailable or changed. Refresh before retrying.");}
 private FeedPage page(List<PostRow> rows){return new FeedPage(rows.stream().limit(20).map(PostRow::post).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null);}
 public FeedPage feed(String subject,Long before,FeedScope scope){ContentFailures.cursor(before);return page(store.list(subject,before,false,scope==FeedScope.FOLLOWING,scope==FeedScope.HIDDEN));}
 public FeedPage saved(String subject,Long before){ContentFailures.cursor(before);return page(store.list(subject,before,true,false,false));}
 public FeedPage search(String subject,String query,Long before){
  query=query.strip();if(query.length()<2||query.length()>80||before!=null&&before<1)throw new UseCaseFailure(INVALID,"Invalid search query or cursor");return page(store.search(subject,query,before));
 }
 public FeedPage activity(String subject,UUID id,Long before){ContentFailures.cursor(before);return page(store.activity(subject,id,before));}
 public Post get(String subject,UUID id){return store.read(subject,id).orElseThrow(ContentFailures::unavailable);}
 public Post save(String subject,UUID id,String text,Audience audience,long version){
  ContentFailures.text(text,3000,true);if(audience==null||version<0)throw new UseCaseFailure(INVALID,"Invalid post");String body=text.strip();
  return transactions.write(()->{
   if(version==0){store.insert(subject,id,body,audience);if(!store.matching(subject,id,body,audience))throw conflict();}
   else if(!store.update(subject,id,body,audience,version))throw conflict();
   return get(subject,id);
  });
 }
 public void delete(String subject,UUID id,long version){transactions.write(()->{if(!store.delete(subject,id,version))throw conflict();return null;});}
 private void marker(String subject,UUID id,Marker marker,boolean enabled){
  if(!access.lockVisible(subject,id))throw ContentFailures.unavailable();
  if(enabled&&!store.profileExists(subject))throw conflict();store.marker(subject,id,marker,enabled);
 }
 public void hidden(String subject,UUID id,boolean hidden){transactions.write(()->{marker(subject,id,Marker.HIDDEN,hidden);return null;});}
 public Post react(String subject,UUID id,boolean liked){return transactions.write(()->{marker(subject,id,Marker.LIKED,liked);return get(subject,id);});}
 public Post bookmark(String subject,UUID id,boolean saved){return transactions.write(()->{marker(subject,id,Marker.SAVED,saved);return get(subject,id);});}
}
