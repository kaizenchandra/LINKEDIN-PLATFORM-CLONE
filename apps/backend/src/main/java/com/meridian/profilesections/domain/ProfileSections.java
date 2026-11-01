package com.meridian.profilesections.domain;
import java.time.*;
import java.util.*;
public final class ProfileSections {
 private ProfileSections(){}
 public enum Section { EXPERIENCE, EDUCATION, CERTIFICATION, PROJECT, SKILL, LANGUAGE }
 public enum Proficiency { ELEMENTARY, LIMITED_WORKING, PROFESSIONAL_WORKING, FULL_PROFESSIONAL, NATIVE_BILINGUAL }
 public record Experience(UUID id,String title,String organization,LocalDate startDate,LocalDate endDate,String description,long version){}
 public static void validate(Experience e,Clock clock){
  require(e!=null,"Entry is required");require(e.id()!=null&&e.version()>=0,"Invalid entry identity or revision");
  text(e.title(),120,true);
  text(e.organization(),160,true);
  text(e.description(),2600,false);
  dates(e.startDate(),e.endDate(),clock);
 }
 public record Education(UUID id,String degree,String institution,LocalDate startDate,LocalDate endDate,String description,String fieldOfStudy,long version){}
 public static void validate(Education e,Clock clock){
  require(e!=null,"Entry is required");require(e.id()!=null&&e.version()>=0,"Invalid entry identity or revision");
  text(e.degree(),120,true);
  text(e.institution(),160,true);
  text(e.description(),2600,false);
  text(e.fieldOfStudy(),160,false);
  dates(e.startDate(),e.endDate(),clock);
 }
 public record Certification(UUID id,String name,String issuer,LocalDate issueDate,LocalDate expiryDate,String credentialUrl,String credentialId,long version){}
 public static void validate(Certification e,Clock clock){
  require(e!=null,"Entry is required");require(e.id()!=null&&e.version()>=0,"Invalid entry identity or revision");
  text(e.name(),120,true);
  text(e.issuer(),160,true);
  text(e.credentialUrl(),2048,false);
  text(e.credentialId(),160,false);
  dates(e.issueDate(),e.expiryDate(),clock);
  require(https(e.credentialUrl()),"Credential URL must be an absolute HTTPS URL without embedded credentials");
 }
 public record Project(UUID id,String title,String organization,LocalDate startDate,LocalDate endDate,String description,String associatedSkills,long version){}
 public static void validate(Project e,Clock clock){
  require(e!=null,"Entry is required");require(e.id()!=null&&e.version()>=0,"Invalid entry identity or revision");
  text(e.title(),120,true);
  text(e.organization(),160,false);
  text(e.description(),2600,false);
  text(e.associatedSkills(),500,false);
  dates(e.startDate(),e.endDate(),clock);
 }
 public record Skill(UUID id,String name,long version){}
 public static void validate(Skill e,Clock clock){
  require(e!=null,"Entry is required");require(e.id()!=null&&e.version()>=0,"Invalid entry identity or revision");
  text(e.name(),80,true);
 }
 public record Language(UUID id,String name,Proficiency proficiency,long version){}
 public static void validate(Language e,Clock clock){
  require(e!=null,"Entry is required");require(e.id()!=null&&e.version()>=0,"Invalid entry identity or revision");
  text(e.name(),80,true);
  require(e.proficiency()!=null,"Proficiency is required");
 }
 public static String normalize(String value){return value.strip().replaceAll("\\s+"," ");}
 private static void text(String value,int max,boolean required){require(value!=null&&value.length()<=max&&(!required||!value.isBlank()),"Invalid section text");}
 private static void dates(LocalDate start,LocalDate end,Clock clock){require(start!=null&&!start.isAfter(LocalDate.now(clock)),"Start date must be in the past or present");require(end==null||!end.isBefore(start),"End date must not precede start date");}
 private static boolean https(String value){if(value.isBlank())return true;try{var uri=new java.net.URI(value);return "https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null;}catch(java.net.URISyntaxException invalid){return false;}}
 private static void require(boolean valid,String message){if(!valid)throw new IllegalArgumentException(message);}
}
