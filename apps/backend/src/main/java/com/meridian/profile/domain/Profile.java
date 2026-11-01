package com.meridian.profile.domain;
public record Profile(String name,String headline,String location,String about,long version) {
 public void validate(){
  if(name==null||name.isBlank()||name.length()>120||headline==null||headline.length()>220||location==null||location.length()>120||about==null||about.length()>2600||version<0)
   throw new IllegalArgumentException("Invalid profile");
 }
}
