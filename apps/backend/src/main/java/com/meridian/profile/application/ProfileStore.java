package com.meridian.profile.application;
import com.meridian.profile.domain.Profile;
public interface ProfileStore {
 void initialize(String subject,String initialName);
 Profile get(String subject);
 boolean update(String subject,Profile profile);
}
