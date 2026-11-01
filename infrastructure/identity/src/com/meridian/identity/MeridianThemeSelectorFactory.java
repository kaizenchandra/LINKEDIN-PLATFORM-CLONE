package com.meridian.identity;

import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.*;

/** Brand this application's login flows without modifying existing realm users or credentials. */
public final class MeridianThemeSelectorFactory extends DefaultThemeSelectorProviderFactory {
 @Override public String getId(){return "meridian";}
 @Override public ThemeSelectorProvider create(KeycloakSession session){
  return new DefaultThemeSelectorProvider(session){
   @Override public String getThemeName(Theme.Type type){
    var realm=session.getContext().getRealm();
    return type==Theme.Type.LOGIN&&realm!=null&&"meridian".equals(realm.getName())?"meridian":super.getThemeName(type);
   }
  };
 }
}
