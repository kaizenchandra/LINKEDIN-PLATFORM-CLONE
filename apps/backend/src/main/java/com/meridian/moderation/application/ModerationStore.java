package com.meridian.moderation.application;
import com.meridian.moderation.domain.Moderation.*;
import java.util.*;
public interface ModerationStore {
 boolean operator(String subject,boolean lock);
 List<Report> reports(String subject,UUID after);
 Optional<Report> matching(UUID id,String subject,UUID postId,Category category,String details);
 boolean authored(String subject,UUID postId);
 boolean lockProfile(String subject);
 int dailyCount(String subject);
 boolean insert(UUID id,String subject,UUID postId,Category category,String details);
 Report report(UUID id);
 List<Item> queue(UUID after);
 /** Locks post before report, then returns the current record and post revision. */
 Optional<ReviewTarget> lockReport(UUID id);
 boolean auditMatches(UUID id,String subject,Decision decision,String reason,long postVersion);
 void removePost(UUID postId);
 void decide(UUID id,Decision decision);
 void audit(UUID id,String subject,Decision decision,String reason,long postVersion);
}
