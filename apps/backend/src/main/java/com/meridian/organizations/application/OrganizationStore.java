package com.meridian.organizations.application;
import com.meridian.organizations.domain.Organizations.*;
import java.util.*;
public interface OrganizationStore {
 boolean initialized(String subject);
 List<Organization> list(String subject,String query,View view,UUID after);
 Optional<Organization> get(String subject,UUID id);
 boolean insert(String subject,UUID id,Write value);
 Optional<String> lockOwner(UUID id);
 boolean update(UUID id,Write value);
 void follow(String subject,UUID id);
 void unfollow(String subject,UUID id);
}
