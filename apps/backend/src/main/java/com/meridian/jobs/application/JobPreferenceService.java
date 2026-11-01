package com.meridian.jobs.application;
import com.meridian.jobs.domain.JobPreferences;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class JobPreferenceService implements JobPreferenceUseCases {
 private final JobPreferenceStore store;private final Transactions transactions;
 public JobPreferenceService(JobPreferenceStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public JobPreferences get(String subject){return store.get(subject).orElse(new JobPreferences("","",null,0));}
 public JobPreferences save(String subject,JobPreferences value){
  final JobPreferences normalized;try{normalized=value.normalized();}catch(IllegalArgumentException invalid){throw new UseCaseFailure(INVALID,invalid.getMessage());}
  return transactions.write(()->{if(!store.save(subject,normalized))throw new UseCaseFailure(CONFLICT,"Preferences changed or profile is unavailable; reload before retrying");return get(subject);});
 }
}
