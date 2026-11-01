package com.meridian.organizations.application;
import com.meridian.organizations.domain.Organizations.*;
import java.util.*;
public interface OrganizationUseCases {
 Page list(String subject,String query,View view,UUID after);
 Organization get(String subject,UUID id);
 Organization save(String subject,UUID id,Write input);
 void follow(String subject,UUID id);
 void unfollow(String subject,UUID id);
}
