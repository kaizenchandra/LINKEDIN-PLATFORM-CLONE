package com.meridian.recruiting.application;
import java.util.UUID;
import com.meridian.recruiting.domain.RecruiterAccess.*;
public interface RecruiterUseCases {
 Page list(String subject,UUID organization,UUID after);
 Access invite(String subject,UUID organization,UUID id,UUID connection);
 Access change(String subject,UUID id,Action action,long version);
}
