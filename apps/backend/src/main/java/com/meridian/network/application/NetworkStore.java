package com.meridian.network.application;
import com.meridian.network.domain.Network.*;
import java.util.*;
/** All pair mutations run inside a transaction, after the shared pair lock. */
public interface NetworkStore {
 List<Connection> connections(String subject);
 Optional<String> target(UUID id);
 void lockPair(String first,String second);
 boolean blocked(String first,String second);
 boolean invitationExists(UUID id,String sender,String recipient);
 boolean invite(UUID id,String sender,UUID recipientId);
 Optional<Pair> ownedPair(UUID id,String subject);
 boolean transitionCompleted(UUID id,String subject,Action action,long nextVersion);
 boolean transition(UUID id,String subject,Action action,long version);
 void enqueueAccepted(UUID id);
 List<FollowRow> following(String subject,Long before);
 long followingCount(String subject);
 boolean followed(String subject,String target);
 boolean visible(String subject);
 boolean profileExists(String subject);
 void lockProfileVisibility(String target);
 void follow(String subject,String target,boolean following);
 List<Block> blocks(String subject);
 Optional<String> blockableTarget(String subject,UUID id);
 void block(String subject,String target);
 void unblock(String subject,String target);
}
