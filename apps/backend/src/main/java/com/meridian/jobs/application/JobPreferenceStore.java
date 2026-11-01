package com.meridian.jobs.application;
import com.meridian.jobs.domain.JobPreferences;
import java.util.Optional;
public interface JobPreferenceStore {
 Optional<JobPreferences> get(String subject);
 boolean save(String subject,JobPreferences value);
}
