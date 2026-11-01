package com.meridian.searchhistory.application;
import com.meridian.searchhistory.domain.SearchHistory.*;
import java.util.UUID;
public interface SearchHistoryUseCases {
 State get(String subject);
 State preference(String subject,boolean enabled,long version);
 State remember(String subject,Kind kind,String query,String location,long version);
 State clear(String subject,long version);
 State remove(String subject,UUID id,long version);
 int purgeExpired();
}
