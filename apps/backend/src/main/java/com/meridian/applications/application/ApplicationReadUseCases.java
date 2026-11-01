package com.meridian.applications.application;
import java.util.UUID;
import com.meridian.applications.domain.Applications.*;
public interface ApplicationReadUseCases {
 Page list(String subject,View view,UUID job,State status,UUID after);
 History history(String subject,UUID id);
 String resume(String subject,UUID id);
}
