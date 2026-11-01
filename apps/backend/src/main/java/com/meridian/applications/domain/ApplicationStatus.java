package com.meridian.applications.domain;
import com.meridian.applications.domain.Applications.State;
public record ApplicationStatus(String applicant,State state,long version) {
 public boolean actorAllowed(String subject,State target){return subject.equals(applicant)?target==State.WITHDRAWN:target!=State.WITHDRAWN;}
 public boolean canTransition(String subject,State target){return subject.equals(applicant)?(state==State.SUBMITTED||state==State.REVIEWING):(state==State.SUBMITTED&&(target==State.REVIEWING||target==State.REJECTED)||state==State.REVIEWING&&(target==State.ACCEPTED||target==State.REJECTED));}
}
