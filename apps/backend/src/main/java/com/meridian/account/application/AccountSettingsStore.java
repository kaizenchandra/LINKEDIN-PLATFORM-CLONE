package com.meridian.account.application;
import com.meridian.account.domain.AccountSettings.*;
import java.util.Optional;
import java.util.UUID;
public interface AccountSettingsStore {
 Optional<Preferences> initializePreferences(String subject);
 boolean updatePreferences(String subject,Theme theme,long version);
 Optional<Visibility> visibility(String subject);
 boolean updateVisibility(String subject,boolean memberVisible,Boolean publicVisible,long version);
 /** Filter by public/member/owner visibility AND bidirectional blocking in the same query. */
 Optional<Summary> visibleProfile(UUID id,String viewer);
}
