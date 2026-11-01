package com.meridian.applications.application;
import java.util.*;
import com.meridian.applications.domain.Applications.*;
public interface ApplicationReadStore {
 List<Application> list(String subject,View view,UUID job,State status,UUID after);
 List<HistoryRow> history(String subject,UUID id);
 Optional<String> resume(String subject,UUID id);
}
