package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class DraftService implements DraftUseCases {
 private final DraftStore store;private final PostUseCases posts;private final Transactions transactions;
 public DraftService(DraftStore store,PostUseCases posts,Transactions transactions){this.store=store;this.posts=posts;this.transactions=transactions;}
 private UseCaseFailure conflict(){return new UseCaseFailure(CONFLICT,"Draft changed, unavailable, empty for publication or capacity reached. Reload before retrying.");}
 private Draft owned(String subject,UUID id,boolean lock){return store.owned(subject,id,lock).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Draft unavailable"));}
 public DraftPage list(String subject,UUID after){var rows=store.list(subject,after);return new DraftPage(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null);}
 public Draft get(String subject,UUID id){return owned(subject,id,false);}
 public Draft save(String subject,UUID id,String text,Audience audience,long version){
  ContentFailures.text(text,3000,false);if(audience==null||version<0)throw new UseCaseFailure(INVALID,"Invalid draft");
  return transactions.write(()->{
   if(version==0){
    if(!store.lockProfile(subject))throw conflict();
    if(store.exists(id)){var draft=owned(subject,id,true);if(!draft.published()&&draft.version()==1&&draft.text().equals(text)&&draft.audience()==audience)return draft;throw conflict();}
    if(store.count(subject)>=100||!store.insert(subject,id,text,audience))throw conflict();
   }else if(!store.update(subject,id,text,audience,version))throw conflict();
   return owned(subject,id,false);
  });
 }
 public void delete(String subject,UUID id,long version){transactions.write(()->{if(!store.delete(subject,id,version))throw conflict();return null;});}
 public Post publish(String subject,UUID id,long version){
  if(version<1)throw new UseCaseFailure(INVALID,"Invalid revision");
  return transactions.write(()->{
   var draft=owned(subject,id,true);
   if(draft.published()){if(version<Long.MAX_VALUE&&draft.version()==version+1)return posts.get(subject,id);throw conflict();}
   if(draft.version()!=version||draft.text().isBlank())throw conflict();
   if(!store.createPost(subject,draft))throw conflict();store.markPublished(id);return posts.get(subject,id);
  });
 }
}
