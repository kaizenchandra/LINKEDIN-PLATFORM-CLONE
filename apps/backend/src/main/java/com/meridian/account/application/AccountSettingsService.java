package com.meridian.account.application;
import com.meridian.account.domain.AccountSettings.*;
import com.meridian.shared.application.*;
import java.util.UUID;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class AccountSettingsService implements AccountSettingsUseCases {
 private final AccountSettingsStore store;private final Transactions transactions;
 public AccountSettingsService(AccountSettingsStore store,Transactions transactions){this.store=store;this.transactions=transactions;}
 public Preferences preferences(String subject){return transactions.write(()->store.initializePreferences(subject).orElseThrow(()->conflict("Initialize profile first")));}
 public Preferences updatePreferences(String subject,Theme theme,long version){
  if(theme==null||version<0)throw new UseCaseFailure(INVALID,"Invalid preferences");
  return transactions.write(()->{
   if(!store.updatePreferences(subject,theme,version))throw conflict("Preferences changed. Reload before retrying.");
   return new Preferences(theme,version+1);
  });
 }
 public Visibility visibility(String subject){return store.visibility(subject).orElseThrow(()->conflict("Initialize profile first"));}
 public Visibility updateVisibility(String subject,boolean memberVisible,Boolean publicVisible,long version){
  if(version<0)throw new UseCaseFailure(INVALID,"Invalid revision");
  return transactions.write(()->{
   if(!store.updateVisibility(subject,memberVisible,publicVisible,version))throw conflict("Visibility changed. Reload before retrying.");
   return visibility(subject);
  });
 }
 public Summary view(UUID id,String viewer){return store.visibleProfile(id,viewer).orElseThrow(()->new UseCaseFailure(viewer==null?UNAUTHENTICATED:NOT_FOUND,"Profile unavailable"));}
 private UseCaseFailure conflict(String message){return new UseCaseFailure(CONFLICT,message);}
}
