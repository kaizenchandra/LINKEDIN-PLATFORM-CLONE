package com.meridian.recruiting.application;
import java.util.*;
import com.meridian.recruiting.domain.RecruiterAccess.*;
public interface RecruiterStore {
 boolean owns(String subject,UUID organization);
 List<Access> list(String subject,UUID organization,UUID after);
 Optional<String> lockOrganization(UUID organization);
 Optional<Invitation> invitation(UUID id);
 Optional<String> connectedMember(UUID connection,String subject);
 void lockPair(String subject,String member);
 boolean acceptedUnblocked(UUID connection,String subject,String member);
 int occupied(UUID organization);
 boolean insert(UUID id,UUID organization,String member,UUID connection);
 Access receipt(UUID id,String subject);
 Optional<UUID> accessibleOrganization(UUID id,String subject);
 Membership lockMembership(UUID id);
 void change(UUID id,String state);
 void audit(UUID id,long version,String subject,String before,String after);
}
