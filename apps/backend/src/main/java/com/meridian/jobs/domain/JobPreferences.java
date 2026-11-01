package com.meridian.jobs.domain;
public record JobPreferences(String query,String location,Workplace workplace,long version){
 public JobPreferences normalized(){
  if(query==null||location==null||query.length()>80||location.length()>120||version<0)throw new IllegalArgumentException("Invalid job preferences");
  return new JobPreferences(query.strip(),location.strip(),workplace,version);
 }
}
