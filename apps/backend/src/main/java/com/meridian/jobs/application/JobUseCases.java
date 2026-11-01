package com.meridian.jobs.application;
import com.meridian.jobs.domain.Jobs.*;
import com.meridian.jobs.domain.Workplace;
import java.util.*;
public interface JobUseCases {
 Page list(String subject,String query,String location,View view,UUID organization,Workplace workplace,UUID after);
 Job get(String subject,UUID id);
 Job publish(String subject,UUID id,Write input);
 void marker(String subject,UUID id,Marker marker,boolean enabled);
}
