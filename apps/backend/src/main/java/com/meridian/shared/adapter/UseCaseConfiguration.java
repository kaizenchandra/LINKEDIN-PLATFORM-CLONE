package com.meridian.shared.adapter;
import com.meridian.shared.application.Transactions;
import com.meridian.account.application.*;
import com.meridian.searchhistory.application.*;
import com.meridian.profilesections.application.*;
import java.time.Clock;
import com.meridian.organizations.application.*;
import com.meridian.peoplesearch.application.*;
import com.meridian.jobs.application.*;
import com.meridian.notifications.application.*;
import com.meridian.moderation.application.*;
import com.meridian.content.application.*;
import com.meridian.network.application.*;
import com.meridian.profile.application.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/** Composition root: only outer code knows concrete application implementations. */
@Configuration
public class UseCaseConfiguration {
 @Bean JobUseCases jobs(JobStore store,Transactions transactions){return new JobService(store,transactions);}
 @Bean OrganizationUseCases organizations(OrganizationStore store,Transactions transactions){return new OrganizationService(store,transactions);}
 @Bean PeopleUseCases people(PeopleStore store,Transactions transactions){return new PeopleService(store,transactions);}
 @Bean JobPreferenceUseCases jobPreferences(JobPreferenceStore store,Transactions transactions){return new JobPreferenceService(store,transactions);}
 @Bean NotificationUseCases notifications(NotificationStore store,NotificationSignals signals,Transactions transactions){return new NotificationService(store,signals,transactions);}
 @Bean ModerationUseCases moderation(ModerationStore store,com.meridian.content.application.ContentAccess access,Transactions transactions){return new ModerationService(store,access,transactions);}
 @Bean PostUseCases posts(PostStore store,ContentAccess access,Transactions transactions){return new PostService(store,access,transactions);}
 @Bean DraftUseCases drafts(DraftStore store,PostUseCases posts,Transactions transactions){return new DraftService(store,posts,transactions);}
 @Bean CommentUseCases comments(CommentStore store,ContentAccess access,Transactions transactions){return new CommentService(store,access,transactions);}
 @Bean NetworkDiscoveryUseCases networkDiscovery(NetworkDiscoveryStore store,Transactions transactions){return new NetworkDiscoveryService(store,transactions);}
 @Bean NetworkUseCases network(NetworkStore store,Transactions transactions,NetworkSignals signals){return new NetworkService(store,transactions,signals);}
 @Bean ProfileUseCases profiles(ProfileStore store,Transactions transactions){return new ProfileService(store,transactions);}
 @Bean ProfileSectionsUseCases profileSections(ProfileSectionsStore store,Transactions transactions){return new ProfileSectionsService(store,transactions,Clock.systemDefaultZone());}
 @Bean SearchHistoryUseCases searchHistory(SearchHistoryStore store,Transactions transactions){return new SearchHistoryService(store,transactions);}
 @Bean AccountSettingsUseCases accountSettings(AccountSettingsStore store,Transactions transactions){return new AccountSettingsService(store,transactions);}
}
