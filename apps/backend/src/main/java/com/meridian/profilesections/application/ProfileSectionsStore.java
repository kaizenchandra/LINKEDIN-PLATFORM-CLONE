package com.meridian.profilesections.application;
import com.meridian.profilesections.domain.ProfileSections.*;
import java.util.*;
/** Owner predicates and revision checks must be atomic; false means unavailable, duplicate or stale. */
public interface ProfileSectionsStore {
 boolean delete(String subject,Section section,UUID id,long version);
 List<Experience> experience(String subject);
 boolean save(String subject,Experience value);
 List<Education> education(String subject);
 boolean save(String subject,Education value);
 List<Certification> certifications(String subject);
 boolean save(String subject,Certification value);
 List<Project> projects(String subject);
 boolean save(String subject,Project value);
 List<Skill> skills(String subject);
 boolean save(String subject,Skill value);
 List<Language> languages(String subject);
 boolean save(String subject,Language value);
}
