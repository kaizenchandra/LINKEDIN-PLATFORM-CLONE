package com.meridian.applications.domain;
import java.util.*;
public final class Applications {
 private Applications(){}
 public enum State {SUBMITTED,REVIEWING,ACCEPTED,REJECTED,WITHDRAWN}
 public enum View {MINE,RECEIVED}
 public record AnswerSnapshot(UUID questionId,String prompt,boolean required,String text){}
 public record Application(UUID id,UUID jobId,String jobTitle,String companyName,String applicantName,String applicantHeadline,String statement,State status,long version,boolean mine,String createdAt,boolean hasResume,long questionVersion,List<AnswerSnapshot> answers){}
 public record Page(List<Application> items,String nextCursor){}
 public record Transition(long version,String actorRole,String fromStatus,String toStatus,String createdAt){}
 public record History(UUID applicationId,long version,List<Transition> items){}
 public record HistoryRow(long currentVersion,Transition transition){}
}
