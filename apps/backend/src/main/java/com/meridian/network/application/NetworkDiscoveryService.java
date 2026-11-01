package com.meridian.network.application;
import com.meridian.network.domain.Network.*;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class NetworkDiscoveryService implements NetworkDiscoveryUseCases {
 private final NetworkDiscoveryStore store;private final Transactions transactions;
 public NetworkDiscoveryService(NetworkDiscoveryStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public Sharing privacy(String subject){return store.privacy(subject).orElseThrow(()->new UseCaseFailure(CONFLICT,"Initialize your profile first"));}
 public Sharing privacy(String subject,boolean visible,long version){
  if(version<0)throw new UseCaseFailure(INVALID,"Invalid revision");
  return transactions.write(()->{if(!store.privacy(subject,visible,version))throw new UseCaseFailure(CONFLICT,"Connection privacy changed. Reload before retrying.");return privacy(subject);});
 }
 private Page page(List<SuggestedMember> rows){return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).memberId().toString():null);}
 public Page suggestions(String subject,UUID after){return page(store.suggestions(subject,after));}
 public Page mutual(String subject,UUID id,UUID after){return page(store.mutual(subject,id,after).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Mutual connections unavailable")));}
 public FollowPage followers(String subject,Long before){
  if(before!=null&&before<1)throw new UseCaseFailure(INVALID,"Invalid cursor");
  var rows=store.followers(subject,before);long total=store.followerCount(subject);
  return new FollowPage(rows.stream().limit(20).map(FollowRow::member).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null,total);
 }
}
