package com.meridian.searchhistory.application;
import com.meridian.searchhistory.domain.SearchHistory.*;
import java.util.UUID;
/** Owner-scoped persistence. All mutations execute inside Transactions.write. */
public interface SearchHistoryStore {
 State state(String subject);
 boolean lockOwner(String subject);
 void initialize(String subject);
 void purgeOwnerExpired(String subject);
 void preference(String subject,boolean enabled);
 void remember(String subject,Criteria criteria);
 void clear(String subject);
 void remove(String subject,UUID id);
 void advance(String subject);
 int purgeExpired();
}
