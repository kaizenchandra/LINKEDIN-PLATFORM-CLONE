package com.meridian.moderation.application;
import com.meridian.moderation.domain.Moderation.*;
import java.util.*;
public interface ModerationUseCases {
 Access access(String subject);
 Page reports(String subject,UUID after);
 Report submit(String subject,UUID id,UUID postId,Category category,String details);
 ReviewQueue queue(String subject,UUID after);
 Report decide(String subject,UUID id,Decision decision,String reason,long version,long postVersion);
}
