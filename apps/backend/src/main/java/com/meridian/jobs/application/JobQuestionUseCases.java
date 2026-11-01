package com.meridian.jobs.application;
import java.util.UUID;
import com.meridian.jobs.domain.JobQuestions.Form;
public interface JobQuestionUseCases {Form get(UUID job);Form save(String subject,UUID job,Form input);}
