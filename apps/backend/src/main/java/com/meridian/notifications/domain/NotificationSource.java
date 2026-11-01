package com.meridian.notifications.domain;
import java.util.Set;
public record NotificationSource(String applicant,String owner,String actor,String status){
 public String recipient(){return Set.of("SUBMITTED","WITHDRAWN").contains(status)?owner:applicant;}
}
