package com.meridian.jobs.domain;
import java.util.*;
public final class Jobs {
 private Jobs(){}
 public enum Status {OPEN,CLOSED}
 public enum View {OPEN,MANAGED,SAVED,PREFERRED,DISMISSED}
 public enum Marker {SAVED,DISMISSED}
 public record Write(UUID organizationId,String title,String location,String description,Workplace workplace,Status status,long version){
  public Write normalized(){
   if(organizationId==null||title==null||title.isBlank()||title.length()>160||location==null||location.isBlank()||location.length()>120||description==null||description.isBlank()||description.length()>10000||workplace==null||status==null||version<0)throw new IllegalArgumentException("Invalid job details");
   return new Write(organizationId,title.strip(),location.strip(),description.strip(),workplace,status,version);
  }
  public boolean matchesCreation(Job stored){return stored.version()==1&&stored.title().equals(title)&&stored.location().equals(location)&&stored.description().equals(description)&&stored.workplace()==workplace&&stored.status()==status;}
 }
 public record Job(UUID id,UUID organizationId,String companyName,String title,String location,String description,Workplace workplace,Status status,long version,boolean owned,boolean saved,boolean canManage){}
 public record Page(List<Job> items,String nextCursor){}
}
