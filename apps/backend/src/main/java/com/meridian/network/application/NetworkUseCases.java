package com.meridian.network.application;
import com.meridian.network.domain.Network.*;
import java.util.*;
public interface NetworkUseCases {
 List<Connection> connections(String subject);
 void invite(String subject,UUID id,UUID recipientId);
 void transition(String subject,UUID id,Action action,long version);
 FollowPage following(String subject,Long before);
 FollowState followState(String subject,UUID id);
 FollowState follow(String subject,UUID id,boolean following);
 List<Block> blocks(String subject);
 void block(String subject,UUID id);
 void unblock(String subject,UUID id);
}
