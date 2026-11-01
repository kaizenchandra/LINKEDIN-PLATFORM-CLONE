package com.meridian.jobs.domain;
import java.util.*;
public final class JobQuestions {
 private JobQuestions(){}
 public record Question(UUID id,String prompt,Boolean required){}
 public record Form(long version,List<Question> questions){}
 public static List<Question> normalize(List<Question> questions){
  if(questions==null||questions.size()>5)throw new IllegalArgumentException("Invalid questions");
  Set<UUID> ids=new HashSet<>();
  for(var q:questions){
   if(q==null||q.id()==null||q.prompt()==null||q.prompt().isBlank()||q.prompt().length()>300||q.required()==null)throw new IllegalArgumentException("Invalid question");
   if(!ids.add(q.id()))throw new IllegalArgumentException("Duplicate question ID");
  }
  return questions.stream().map(q->new Question(q.id(),text(q.prompt()),q.required())).toList();
 }
 private static String text(String value){if(value.codePoints().anyMatch(c->c>=0xD800&&c<=0xDFFF||Character.isISOControl(c)&&c!=10&&c!=13&&c!=9))throw new IllegalArgumentException("Invalid text characters");return value.strip();}
}
