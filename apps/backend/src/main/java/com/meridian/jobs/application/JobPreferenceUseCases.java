package com.meridian.jobs.application;
import com.meridian.jobs.domain.JobPreferences;
public interface JobPreferenceUseCases {
 JobPreferences get(String subject);
 JobPreferences save(String subject,JobPreferences value);
}
