package com.meridian.identity;

import org.keycloak.Config;
import org.keycloak.events.*;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;

/** Versioned development-realm migration, explicitly disabled outside local Compose. */
public final class LocalIdentityPolicyFactory implements EventListenerProviderFactory {
 public String getId(){return "meridian-local-policy";}
 public void init(Config.Scope config){}
 public void close(){}
 public EventListenerProvider create(KeycloakSession session){return new EventListenerProvider(){
  public void onEvent(Event event){}
  public void onEvent(AdminEvent event,boolean includeRepresentation){}
  public void close(){}
 };}
 public void postInit(KeycloakSessionFactory factory){
  if(!"true".equals(System.getenv("MERIDIAN_LOCAL_IDENTITY_POLICY")))return;
  factory.register(event->{if(event instanceof PostMigrationEvent)KeycloakModelUtils.runJobInTransaction(factory,session->{
   var realm=session.realms().getRealmByName("meridian");
   if(realm==null)return;
   var client=realm.getClientByClientId("meridian");
   if(client!=null){
    client.setAttribute("backchannel.logout.url",System.getenv().getOrDefault("MERIDIAN_LOCAL_BACKCHANNEL_URL","http://host.docker.internal:8081/logout/connect/back-channel/keycloak"));
    client.setAttribute("backchannel.logout.session.required","true");
    client.setAttribute("backchannel.logout.revoke.offline.tokens","false");
   }
   if("1".equals(realm.getAttribute("meridian.local-security-policy")))return;
   var policy=realm.getWebAuthnPolicyPasswordless();
   policy.setRpEntityName("Meridian");policy.setRpId("localhost");
   policy.setUserVerificationRequirement("required");policy.setRequireResidentKey("Yes");
   policy.setCreateTimeout(120);policy.setPasskeysEnabled(true);
   realm.setWebAuthnPolicyPasswordless(policy);
   for(String alias:new String[]{"webauthn-register-passwordless","CONFIGURE_RECOVERY_AUTHN_CODES"}){
    var action=realm.getRequiredActionProviderByAlias(alias);
    if(action==null)throw new IllegalStateException("Missing native identity action: "+alias);
    action.setEnabled(true);action.setDefaultAction(false);realm.updateRequiredActionProvider(action);
   }
   // Enable the native backup factor only in the default browser flow.
   var browser=realm.getBrowserFlow();
   if(browser==null)throw new IllegalStateException("Missing browser flow");
   enableRecovery(realm,browser.getId());
   realm.setAttribute("meridian.local-security-policy","1");
  });});
 }
 private void enableRecovery(RealmModel realm,String flow){
  realm.getAuthenticationExecutionsStream(flow).toList().forEach(execution->{
   if(execution.isAuthenticatorFlow())enableRecovery(realm,execution.getFlowId());
   else if("auth-recovery-authn-code-form".equals(execution.getAuthenticator())){
    execution.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE);
    realm.updateAuthenticatorExecution(execution);
   }
  });
 }
}
