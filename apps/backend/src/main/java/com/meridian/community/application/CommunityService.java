package com.meridian.community.application;
import com.meridian.community.domain.Community.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
import java.util.*;
public final class CommunityService {
 private final CommunityStore store;private final Transactions transactions;
 public CommunityService(CommunityStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public List<Update> catchUp(String subject){return store.catchUp(subject);}
 public List<Entry> list(String subject,Kind kind){return store.list(subject,kind);}
 public void create(String subject,Draft d){
  if(d.id()==null||d.kind()==null||d.title()==null||d.title().isBlank()||d.title().length()>160||d.description()==null||d.description().length()>3000||d.location()==null||d.location().length()>240||d.kind()==Kind.EVENT&&d.startsAt()==null)throw new UseCaseFailure(INVALID,"Check the title, description and event details");
  transactions.write(()->{if(!store.profileExists(subject))throw new UseCaseFailure(CONFLICT,"Initialize your profile first");if(!store.create(subject,d))throw new UseCaseFailure(CONFLICT,"Listing ID already used");return null;});
 }
 public void membership(String subject,UUID id,boolean joined){transactions.write(()->{if(!store.profileExists(subject))throw new UseCaseFailure(CONFLICT,"Initialize your profile first");if(!store.exists(id))throw new UseCaseFailure(NOT_FOUND,"Listing unavailable");store.membership(subject,id,joined);return null;});}
}
