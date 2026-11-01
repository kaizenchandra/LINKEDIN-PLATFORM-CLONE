package com.meridian.jobs.application;
import java.util.*;
import com.meridian.jobs.domain.JobQuestions.*;
public interface JobQuestionStore {
 Optional<UUID> organization(UUID job);
 boolean lockOrganization(UUID organization);
 boolean canRecruit(UUID organization,String subject);
 void lockJob(UUID job);
 Form load(UUID job);
 void save(UUID job,Form current,List<Question> questions);
}
