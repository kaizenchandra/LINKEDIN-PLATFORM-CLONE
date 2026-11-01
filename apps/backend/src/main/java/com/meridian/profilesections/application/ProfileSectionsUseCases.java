package com.meridian.profilesections.application;
import com.meridian.profilesections.domain.ProfileSections.*;
import java.util.*;
public interface ProfileSectionsUseCases {
 void delete(String subject,Section section,UUID id,long version);
 List<Experience> experience(String subject);
 Experience save(String subject,Experience value);
 List<Education> education(String subject);
 Education save(String subject,Education value);
 List<Certification> certifications(String subject);
 Certification save(String subject,Certification value);
 List<Project> projects(String subject);
 Project save(String subject,Project value);
 List<Skill> skills(String subject);
 Skill save(String subject,Skill value);
 List<Language> languages(String subject);
 Language save(String subject,Language value);
}
