package com.meridian.searchhistory.domain;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
public final class SearchHistory {
 private SearchHistory(){}
 public enum Kind {people,companies,jobs,posts}
 public record Entry(UUID id,Kind kind,String query,String location,String searchedAt){}
 public record State(boolean enabled,long version,List<Entry> items){
  public State{items=List.copyOf(items);}
 }
 public record Criteria(Kind kind,String query,String location){
  public Criteria{
   if(kind==null||query==null||location==null||query.length()<2||query.length()>80||location.length()>120)
    throw new IllegalArgumentException("Invalid search criteria");
   query=normalize(query);location=kind==Kind.people||kind==Kind.jobs?normalize(location):"";
   if(query.length()<2)throw new IllegalArgumentException("Enter at least two search characters");
  }
  public String queryKey(){return query.toLowerCase(Locale.ROOT);}
  public String locationKey(){return location.toLowerCase(Locale.ROOT);}
 }
 private static String normalize(String value){return value.strip().replaceAll("\\s+"," ");}
}
