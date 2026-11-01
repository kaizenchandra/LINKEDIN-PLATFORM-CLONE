package com.meridian.applications.application;
import java.util.UUID;
import com.meridian.applications.domain.Applications.*;
public interface ApplicationStatusUseCases {Application change(String subject,UUID id,State target,long version);}
