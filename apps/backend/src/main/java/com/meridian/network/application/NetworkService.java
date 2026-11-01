package com.meridian.network.application;
import com.meridian.network.domain.Network.*;
import com.meridian.shared.application.*;
import java.util.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class NetworkService implements NetworkUseCases {
 private final NetworkStore store;private final Transactions transactions;private final NetworkSignals signals;
 public NetworkService(NetworkStore store,Transactions transactions,NetworkSignals signals){this.store=store;this.transactions=transactions;this.signals=signals;}
 private UseCaseFailure conflict(){return new UseCaseFailure(CONFLICT,"Request unavailable, duplicate or changed. Reload your network.");}
 private UseCaseFailure unavailable(){return new UseCaseFailure(NOT_FOUND,"Member unavailable");}
 public List<Connection> connections(String subject){return store.connections(subject);}
 public void invite(String subject,UUID id,UUID recipientId){transactions.write(()->{
  String target=store.target(recipientId).orElseThrow(this::conflict);store.lockPair(subject,target);
  if(store.blocked(subject,target))throw conflict();
  if(store.invitationExists(id,subject,target))return null;
  if(!store.invite(id,subject,recipientId))throw conflict();return null;
 });}
 public void transition(String subject,UUID id,Action action,long version){
  if(action==null||version<0)throw new UseCaseFailure(INVALID,"Invalid transition");
  transactions.write(()->{
   var pair=store.ownedPair(id,subject).orElseThrow(this::conflict);store.lockPair(pair.sender(),pair.recipient());
   if(!action.permits(subject,pair))throw conflict();
   if(version<Long.MAX_VALUE&&store.transitionCompleted(id,subject,action,version+1))return null;
   if(!store.transition(id,subject,action,version))throw conflict();
   signals.changed(pair.subjects());if(action==Action.ACCEPT)store.enqueueAccepted(id);return null;
  });
 }
 public FollowPage following(String subject,Long before){
  if(before!=null&&before<1)throw new UseCaseFailure(INVALID,"Invalid cursor");
  var rows=store.following(subject,before);long total=store.followingCount(subject);
  return new FollowPage(rows.stream().limit(20).map(FollowRow::member).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null,total);
 }
 private String other(String subject,UUID id){return store.target(id).filter(target->!target.equals(subject)).orElseThrow(this::unavailable);}
 private void permitted(String subject,String target){if(store.blocked(subject,target))throw unavailable();}
 public FollowState followState(String subject,UUID id){
  String target=other(subject,id);permitted(subject,target);boolean following=store.followed(subject,target);
  if(!following&&!store.visible(target))throw unavailable();return new FollowState(following);
 }
 public FollowState follow(String subject,UUID id,boolean following){return transactions.write(()->{
  String target=other(subject,id);store.lockPair(subject,target);store.lockProfileVisibility(target);permitted(subject,target);
  if(following&&(!store.visible(target)||!store.profileExists(subject)))throw unavailable();
  store.follow(subject,target,following);return new FollowState(following);
 });}
 public List<Block> blocks(String subject){return store.blocks(subject);}
 public void block(String subject,UUID id){transactions.write(()->{
  String target=store.blockableTarget(subject,id).orElseThrow(this::unavailable);store.lockPair(subject,target);
  store.block(subject,target);signals.changed(Set.of(subject,target));return null;
 });}
 public void unblock(String subject,UUID id){transactions.write(()->{
  var target=store.target(id);if(target.isEmpty())return null;store.lockPair(subject,target.get());
  store.unblock(subject,target.get());signals.changed(Set.of(subject,target.get()));return null;
 });}
}
