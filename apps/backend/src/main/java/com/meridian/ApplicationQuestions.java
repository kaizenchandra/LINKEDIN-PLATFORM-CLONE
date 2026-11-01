package com.meridian;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
final class ApplicationQuestions {
 private ApplicationQuestions(){}
 private static final ObjectMapper JSON=new ObjectMapper();
 record Question(@NotNull UUID id,@NotBlank @Size(max=300) String prompt,@NotNull Boolean required){}
 record Form(@NotNull @Min(0) Long version,@NotNull @Size(max=5) List<@NotNull @Valid Question> questions){}
 record Answer(@NotNull UUID questionId,@NotNull @Size(max=1000) String text){}
 record Snapshot(UUID questionId,String prompt,boolean required,String text){}
 static String encode(Object value){try{return JSON.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Question serialization failed");}}
 static List<Question> questions(String value){try{return Arrays.asList(JSON.readValue(value,Question[].class));}catch(Exception e){throw new IllegalStateException("Invalid stored question form");}}
 static List<Snapshot> snapshots(String value){try{return Arrays.asList(JSON.readValue(value,Snapshot[].class));}catch(Exception e){throw new IllegalStateException("Invalid stored answers");}}
 static Form load(JdbcTemplate db,UUID job){
  var rows=db.query("select version,questions::text from job_question_form where job_id=?",(r,i)->new Form(r.getLong(1),questions(r.getString(2))),job);
  return rows.isEmpty()?new Form(0L,List.of()):rows.getFirst();
 }
 static String text(String value){if(value.codePoints().anyMatch(c->c>=0xD800&&c<=0xDFFF||Character.isISOControl(c)&&c!=10&&c!=13&&c!=9))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid text characters");return value.strip();}
 static List<Question> normalize(List<Question> questions){
  Set<UUID> ids=new HashSet<>();for(var question:questions)if(!ids.add(question.id()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Duplicate question ID");
  return questions.stream().map(q->new Question(q.id(),text(q.prompt()),q.required())).toList();
 }
 static List<Snapshot> answer(Form form,List<Answer> supplied){
  Map<UUID,String> values=new HashMap<>();var ids=form.questions().stream().map(Question::id).collect(java.util.stream.Collectors.toSet());
  for(var answer:supplied==null?List.<Answer>of():supplied)if(!ids.contains(answer.questionId())||values.putIfAbsent(answer.questionId(),text(answer.text()))!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown or duplicate question");
  return form.questions().stream().map(q->{String text=values.getOrDefault(q.id(),"");if(q.required()&&text.isBlank())throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Answer every required application question");return new Snapshot(q.id(),q.prompt(),q.required(),text);}).toList();
 }
}
