package com.meridian.account.application;
import com.meridian.account.domain.AccountSettings.*;
import java.util.UUID;
public interface AccountSettingsUseCases {
 Preferences preferences(String subject);
 Preferences updatePreferences(String subject,Theme theme,long version);
 Visibility visibility(String subject);
 Visibility updateVisibility(String subject,boolean memberVisible,Boolean publicVisible,long version);
 Summary view(UUID id,String viewer);
}
