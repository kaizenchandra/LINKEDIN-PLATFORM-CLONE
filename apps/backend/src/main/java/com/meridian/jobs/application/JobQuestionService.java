package com.meridian.jobs.application;
import java.util.*;
import com.meridian.jobs.domain.JobQuestions;
import com.meridian.jobs.domain.JobQuestions.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class JobQuestionService implements JobQuestionUseCases {
 private final JobQuestionStore store;private final Transactions transactions;
 public JobQuestionService(JobQuestionStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 private UUID organization(UUID job){return store.organization(job).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Job unavailable"));}
 public Form get(UUID job){organization(job);return store.load(job);}
 public Form save(String subject,UUID job,Form input){
  if(input==null||input.version()<0)throw new UseCaseFailure(INVALID,"Invalid question form");
  return transactions.write(()->{
   UUID company=organization(job);
   if(!store.lockOrganization(company))throw new UseCaseFailure(NOT_FOUND,"Company unavailable");
   if(!store.canRecruit(company,subject))throw new UseCaseFailure(FORBIDDEN,"Recruiting access required");
   store.lockJob(job);var current=store.load(job);
   if(input.version()!=current.version())throw new UseCaseFailure(CONFLICT,"Questions changed; reload before retrying");
   final List<Question> questions;try{questions=JobQuestions.normalize(input.questions());}catch(IllegalArgumentException invalid){throw new UseCaseFailure(INVALID,invalid.getMessage());}
   store.save(job,current,questions);return get(job);
  });
 }
}
