package com.meridian.profile.application;
import com.meridian.profile.domain.Profile;
import com.meridian.shared.application.*;
public final class ProfileService implements ProfileUseCases {
 private final ProfileStore store;private final Transactions transactions;
 public ProfileService(ProfileStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public Profile me(String subject,String initialName){return transactions.write(()->{
  store.initialize(subject,initialName==null?"New member":initialName);return store.get(subject);
 });}
 public Profile update(String subject,Profile profile){
  try{profile.validate();}catch(IllegalArgumentException failure){throw new UseCaseFailure(UseCaseFailure.Reason.INVALID,failure.getMessage());}
  return transactions.write(()->{
   if(!store.update(subject,profile))throw new UseCaseFailure(UseCaseFailure.Reason.CONFLICT,"Profile changed; reload before saving");
   return store.get(subject);
  });
 }
}
