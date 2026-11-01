package com.meridian.jobs.application;
import com.meridian.jobs.domain.Jobs.*;
import com.meridian.jobs.domain.Workplace;
import java.util.*;
public interface JobStore {
 List<Job> list(String subject,String query,String location,View view,UUID organization,Workplace workplace,UUID after);
 Optional<Job> get(String subject,UUID id);
 boolean lockOrganization(UUID organization);
 boolean recruitingAllowed(String subject,UUID organization);
 boolean insert(UUID id,Write value);
 Optional<UUID> lockJobOrganization(UUID id);
 boolean update(UUID id,Write value);
 boolean initialized(String subject);
 void marker(String subject,UUID id,Marker marker,boolean enabled);
}
