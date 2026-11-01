package com.meridian.applications.application;
import java.util.*;
import com.meridian.applications.domain.Submission.*;
import com.meridian.applications.domain.Applications.*;
import com.meridian.jobs.domain.JobQuestions.Form;
public interface ApplicationSubmissionStore {
 Optional<UUID> organization(UUID job);
 boolean lockOrganization(UUID organization);
 Optional<String> lockJob(UUID job);
 Optional<Previous> previous(UUID job,String subject);
 Form questions(UUID job);
 boolean canRecruit(UUID organization,String subject);
 Optional<Profile> profile(String subject);
 void insert(UUID id,UUID job,String subject,Profile profile,String statement,long questionVersion,List<AnswerSnapshot> answers,String resume);
 void auditAndOutbox(UUID id,String subject);
 Set<String> audience(UUID organization,String subject);
 Application receipt(UUID id,String subject);
}
