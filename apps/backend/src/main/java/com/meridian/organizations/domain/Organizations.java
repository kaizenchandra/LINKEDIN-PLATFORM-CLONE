package com.meridian.organizations.domain;
import java.util.*;
public final class Organizations {
 private Organizations(){}
 public enum View {ALL,MANAGED,FOLLOWING}
 public record Organization(UUID id,String name,String industry,String location,String about,long version,boolean owned,boolean following,long followerCount,boolean canRecruit){}
 public record Page(List<Organization> items,String nextCursor){}
 public record Write(String name,String industry,String location,String about,long version){
  public Write normalized(){
   if(name==null||name.isBlank()||name.length()>160||industry==null||industry.length()>120||location==null||location.length()>120||about==null||about.length()>3000||version<0)throw new IllegalArgumentException("Invalid company details");
   return new Write(name.strip(),industry.strip(),location.strip(),about.strip(),version);
  }
  public boolean matchesCreation(Organization stored){return stored.version()==1&&stored.name().equals(name)&&stored.industry().equals(industry)&&stored.location().equals(location)&&stored.about().equals(about);}
 }
}
