package com.meridian.peoplesearch.domain;
import java.util.*;
public final class People {
 private People(){}
 public record Discovery(boolean discoverable,long version){}
 public record Person(UUID memberId,String name,String headline,String location){}
 public record Page(List<Person> items,String nextCursor){}
 public record Criteria(String query,String location){
  public Criteria{query=query.strip();location=location.strip();if(query.length()<2||query.length()>80||location.length()>120)throw new IllegalArgumentException("Enter 2–80 search characters and at most 120 location characters");}
 }
}
