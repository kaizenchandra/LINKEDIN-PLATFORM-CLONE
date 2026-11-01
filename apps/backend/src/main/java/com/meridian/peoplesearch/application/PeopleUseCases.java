package com.meridian.peoplesearch.application;
import com.meridian.peoplesearch.domain.People.*;
import java.util.*;
public interface PeopleUseCases {
 Discovery discovery(String subject);
 Discovery discovery(String subject,boolean discoverable,long version);
 Page search(String subject,String query,String location,UUID after);
}
