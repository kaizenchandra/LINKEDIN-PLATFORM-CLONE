package com.meridian.peoplesearch.application;
import com.meridian.peoplesearch.domain.People.*;
import java.util.*;
public interface PeopleStore {
 Optional<Discovery> discovery(String subject);
 boolean discovery(String subject,boolean discoverable,long version);
 /** Must enforce member visibility, discovery consent, self-exclusion and both block directions. */
 List<Person> search(String subject,Criteria criteria,UUID after);
}
