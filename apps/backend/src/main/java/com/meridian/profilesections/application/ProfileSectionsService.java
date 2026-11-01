package com.meridian.profilesections.application;
import com.meridian.profilesections.domain.ProfileSections.*;
import java.util.*;
import com.meridian.profilesections.domain.ProfileSections;
import com.meridian.shared.application.*;
import java.time.Clock;
public final class ProfileSectionsService implements ProfileSectionsUseCases {
 private final ProfileSectionsStore store;private final Transactions transactions;private final Clock clock;
 public ProfileSectionsService(ProfileSectionsStore store,Transactions transactions,Clock clock){this.store=store;this.transactions=transactions;this.clock=clock;}
 private UseCaseFailure conflict(Section section){return new UseCaseFailure(UseCaseFailure.Reason.CONFLICT,switch(section){
  case SKILL->"Skill already exists, changed or is unavailable. Reload before retrying.";
  case LANGUAGE->"Language already exists, changed or is unavailable. Reload before retrying.";
  default->"Entry unavailable or changed. Reload before retrying.";
 });}
 private void validate(Runnable validation){try{validation.run();}catch(IllegalArgumentException failure){throw new UseCaseFailure(UseCaseFailure.Reason.INVALID,failure.getMessage());}}
 public void delete(String subject,Section section,UUID id,long version){transactions.write(()->{if(!store.delete(subject,section,id,version))throw conflict(section);return null;});}
 public List<Experience> experience(String subject){return store.experience(subject);}
 public Experience save(String subject,Experience value){
  validate(()->ProfileSections.validate(value,clock));
  var normalized=new Experience(value.id(),value.title(),value.organization(),value.startDate(),value.endDate(),value.description(),value.version());
  return transactions.write(()->{if(!store.save(subject,normalized))throw conflict(Section.EXPERIENCE);return new Experience(normalized.id(),normalized.title(),normalized.organization(),normalized.startDate(),normalized.endDate(),normalized.description(),normalized.version()+1);});
 }
 public List<Education> education(String subject){return store.education(subject);}
 public Education save(String subject,Education value){
  validate(()->ProfileSections.validate(value,clock));
  var normalized=new Education(value.id(),value.degree(),value.institution(),value.startDate(),value.endDate(),value.description(),value.fieldOfStudy(),value.version());
  return transactions.write(()->{if(!store.save(subject,normalized))throw conflict(Section.EDUCATION);return new Education(normalized.id(),normalized.degree(),normalized.institution(),normalized.startDate(),normalized.endDate(),normalized.description(),normalized.fieldOfStudy(),normalized.version()+1);});
 }
 public List<Certification> certifications(String subject){return store.certifications(subject);}
 public Certification save(String subject,Certification value){
  validate(()->ProfileSections.validate(value,clock));
  var normalized=new Certification(value.id(),value.name(),value.issuer(),value.issueDate(),value.expiryDate(),value.credentialUrl(),value.credentialId(),value.version());
  return transactions.write(()->{if(!store.save(subject,normalized))throw conflict(Section.CERTIFICATION);return new Certification(normalized.id(),normalized.name(),normalized.issuer(),normalized.issueDate(),normalized.expiryDate(),normalized.credentialUrl(),normalized.credentialId(),normalized.version()+1);});
 }
 public List<Project> projects(String subject){return store.projects(subject);}
 public Project save(String subject,Project value){
  validate(()->ProfileSections.validate(value,clock));
  var normalized=new Project(value.id(),value.title(),value.organization(),value.startDate(),value.endDate(),value.description(),value.associatedSkills(),value.version());
  return transactions.write(()->{if(!store.save(subject,normalized))throw conflict(Section.PROJECT);return new Project(normalized.id(),normalized.title(),normalized.organization(),normalized.startDate(),normalized.endDate(),normalized.description(),normalized.associatedSkills(),normalized.version()+1);});
 }
 public List<Skill> skills(String subject){return store.skills(subject);}
 public Skill save(String subject,Skill value){
  validate(()->ProfileSections.validate(value,clock));
  var normalized=new Skill(value.id(),ProfileSections.normalize(value.name()),value.version());
  return transactions.write(()->{if(!store.save(subject,normalized))throw conflict(Section.SKILL);return new Skill(normalized.id(),normalized.name(),normalized.version()+1);});
 }
 public List<Language> languages(String subject){return store.languages(subject);}
 public Language save(String subject,Language value){
  validate(()->ProfileSections.validate(value,clock));
  var normalized=new Language(value.id(),ProfileSections.normalize(value.name()),value.proficiency(),value.version());
  return transactions.write(()->{if(!store.save(subject,normalized))throw conflict(Section.LANGUAGE);return new Language(normalized.id(),normalized.name(),normalized.proficiency(),normalized.version()+1);});
 }
}
