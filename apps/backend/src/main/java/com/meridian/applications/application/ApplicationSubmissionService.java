package com.meridian.applications.application;
import java.util.*;
import com.meridian.applications.domain.Submission;
import com.meridian.applications.domain.Submission.*;
import com.meridian.applications.domain.Applications.Application;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class ApplicationSubmissionService implements ApplicationSubmissionUseCases {
 private final ApplicationSubmissionStore store;private final ApplicationSignals signals;private final Transactions transactions;
 public ApplicationSubmissionService(ApplicationSubmissionStore store,ApplicationSignals signals,Transactions transactions){this.store=store;this.signals=signals;this.transactions=transactions;}
 public Application submit(String subject,UUID job,Input input){
  if(input==null||input.statement()==null||input.statement().isBlank()||input.statement().length()>4000||input.questionVersion()!=null&&input.questionVersion()<0)throw new UseCaseFailure(INVALID,"Invalid application");
  try{
   String statement=input.statement().strip(),resume=input.resume()==null?null:input.resume().validated();
   return transactions.write(()->{
    UUID organization=store.organization(job).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Job unavailable"));
    if(!store.lockOrganization(organization))throw new UseCaseFailure(NOT_FOUND,"Company unavailable");
    String status=store.lockJob(job).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Job unavailable"));
    var existing=store.previous(job,subject);long version=input.questionVersion()==null?0:input.questionVersion();
    if(existing.isPresent()){
     var previous=existing.get();var form=previous.form();
     if(version!=form.version()||!previous.statement().equals(statement)||!Objects.equals(previous.resume(),resume)||!Submission.answer(form,input.answers()).equals(previous.answers()))throw new UseCaseFailure(CONFLICT,"Application already submitted; submitted content cannot be replaced");
     return store.receipt(previous.id(),subject);
    }
    var form=store.questions(job);if(version!=form.version())throw new UseCaseFailure(CONFLICT,"Application questions changed; reload questions before submitting");
    var answers=Submission.answer(form,input.answers());
    if(store.canRecruit(organization,subject)||!"OPEN".equals(status))throw new UseCaseFailure(CONFLICT,"Job is closed or belongs to your company");
    var profile=store.profile(subject).orElseThrow(()->new UseCaseFailure(CONFLICT,"Initialize profile first"));
    UUID id=UUID.randomUUID();store.insert(id,job,subject,profile,statement,version,answers,resume);store.auditAndOutbox(id,subject);
    signals.changed(store.audience(organization,subject));return store.receipt(id,subject);
   });
  }catch(Submission.RequiredAnswer missing){throw new UseCaseFailure(UNPROCESSABLE,missing.getMessage());}
  catch(IllegalArgumentException invalid){throw new UseCaseFailure(INVALID,invalid.getMessage());}
 }
}
