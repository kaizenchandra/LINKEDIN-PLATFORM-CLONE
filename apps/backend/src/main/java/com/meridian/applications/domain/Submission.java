package com.meridian.applications.domain;
import java.util.*;
import java.nio.charset.StandardCharsets;
import com.meridian.jobs.domain.JobQuestions.*;
import com.meridian.applications.domain.Applications.AnswerSnapshot;
public final class Submission {
 private Submission(){}
 public record Answer(UUID questionId,String text){}
 public record Resume(String fileName,String text){
  public String validated(){
   boolean badName=fileName==null||fileName.isBlank()||fileName.length()>120||!fileName.toLowerCase(Locale.ROOT).endsWith(".txt")||fileName.length()<5||fileName.contains("/")||fileName.contains("\\")||fileName.codePoints().anyMatch(Character::isISOControl);
   boolean badText=text==null||text.isBlank()||text.getBytes(StandardCharsets.UTF_8).length>20480||invalidText(text);
   if(badName||badText)throw new IllegalArgumentException("Use a nonblank UTF-8 .txt resume up to 20 KiB");return text;
  }
 }
 public record Input(String statement,Resume resume,Long questionVersion,List<Answer> answers){}
 public record Previous(UUID id,String statement,long questionVersion,List<AnswerSnapshot> answers,String resume){
  public Form form(){return new Form(questionVersion,answers.stream().map(a->new Question(a.questionId(),a.prompt(),a.required())).toList());}
 }
 public record Profile(String name,String headline){}
 public static final class RequiredAnswer extends IllegalArgumentException {public RequiredAnswer(){super("Answer every required application question");}}
 private static boolean invalidText(String text){return text.codePoints().anyMatch(c->c>=0xD800&&c<=0xDFFF||Character.isISOControl(c)&&c!=10&&c!=13&&c!=9);}
 public static List<AnswerSnapshot> answer(Form form,List<Answer> supplied){
  if(supplied!=null&&supplied.size()>5)throw new IllegalArgumentException("Invalid answers");
  Map<UUID,String> values=new HashMap<>();var ids=form.questions().stream().map(Question::id).collect(java.util.stream.Collectors.toSet());
  for(var a:supplied==null?List.<Answer>of():supplied){
   if(a==null||a.questionId()==null||a.text()==null||a.text().length()>1000)throw new IllegalArgumentException("Invalid answer");
   if(!ids.contains(a.questionId()))throw new IllegalArgumentException("Unknown or duplicate question");
   if(invalidText(a.text()))throw new IllegalArgumentException("Invalid text characters");
   if(values.putIfAbsent(a.questionId(),a.text().strip())!=null)throw new IllegalArgumentException("Unknown or duplicate question");
  }
  return form.questions().stream().map(q->{String text=values.getOrDefault(q.id(),"");if(q.required()&&text.isBlank())throw new RequiredAnswer();return new AnswerSnapshot(q.id(),q.prompt(),q.required(),text);}).toList();
 }
}
