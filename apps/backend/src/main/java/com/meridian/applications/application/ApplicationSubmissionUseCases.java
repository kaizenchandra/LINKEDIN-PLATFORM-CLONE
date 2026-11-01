package com.meridian.applications.application;
import java.util.UUID;
import com.meridian.applications.domain.Submission.Input;
import com.meridian.applications.domain.Applications.Application;
public interface ApplicationSubmissionUseCases {Application submit(String subject,UUID job,Input input);}
