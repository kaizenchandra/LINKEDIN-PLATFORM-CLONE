package com.meridian.profile.application;
import com.meridian.profile.domain.Profile;
public interface ProfileUseCases {
 Profile me(String subject,String initialName);
 Profile update(String subject,Profile profile);
}
