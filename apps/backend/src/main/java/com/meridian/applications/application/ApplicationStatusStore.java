package com.meridian.applications.application;
import java.util.*;
import com.meridian.applications.domain.ApplicationStatus;
import com.meridian.applications.domain.Applications.*;
public interface ApplicationStatusStore {
 Optional<UUID> organization(UUID id,String subject);
 boolean lockOrganization(UUID organization);
 Optional<ApplicationStatus> lockStatus(UUID id,String subject);
 void update(UUID id,State target);
 void auditAndOutbox(UUID id,long version,String subject,State before,State after);
 Set<String> audience(UUID organization,String applicant);
 Application receipt(UUID id,String subject);
}
