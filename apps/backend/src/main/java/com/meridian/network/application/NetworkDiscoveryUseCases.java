package com.meridian.network.application;
import com.meridian.network.domain.Network.*;
import java.util.*;
public interface NetworkDiscoveryUseCases {
 Sharing privacy(String subject);
 Sharing privacy(String subject,boolean visible,long version);
 Page suggestions(String subject,UUID after);
 Page mutual(String subject,UUID id,UUID after);
 FollowPage followers(String subject,Long before);
}
