package com.meridian.network.application;
import com.meridian.network.domain.Network.*;
import java.util.*;
public interface NetworkDiscoveryStore {
 Optional<Sharing> privacy(String subject);
 boolean privacy(String subject,boolean visible,long version);
 List<SuggestedMember> suggestions(String subject,UUID after);
 /** Empty means target consent denied; a present empty list means no mutual members. */
 Optional<List<SuggestedMember>> mutual(String subject,UUID id,UUID after);
 List<FollowRow> followers(String subject,Long before);
 long followerCount(String subject);
}
