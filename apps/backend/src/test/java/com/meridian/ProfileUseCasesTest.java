package com.meridian;
import com.meridian.profile.application.*;
import com.meridian.profile.domain.Profile;
import com.meridian.profilesections.application.*;
import com.meridian.profilesections.domain.ProfileSections.*;
import com.meridian.shared.application.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProfileUseCasesTest {
 private final Transactions transactions=new Transactions(){
  public <T>T write(Supplier<T> work){return work.get();}
  public <T>T snapshot(Supplier<T> work){return work.get();}
 };
 private final Clock clock=Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"),ZoneOffset.UTC);
 @Test void invalidDatesAndCredentialsNeverReachStore(){
  var store=mock(ProfileSectionsStore.class);var service=new ProfileSectionsService(store,transactions,clock);
  var start=LocalDate.of(2026,10,1);
  assertEquals(UseCaseFailure.Reason.INVALID,assertThrows(UseCaseFailure.class,()->service.save("owner",new Experience(UUID.randomUUID(),"Engineer","Company",start,start.minusDays(1),"",0))).reason());
  assertThrows(UseCaseFailure.class,()->service.save("owner",new Certification(UUID.randomUUID(),"Certificate","Issuer",start,null,"https://user:secret@example.com","",0)));
  assertThrows(UseCaseFailure.class,()->service.save("owner",new Education(UUID.randomUUID(),"Degree","School",LocalDate.of(2027,1,1),null,"","",0)));
  verifyNoInteractions(store);
 }
 @Test void skillNormalizationAndRevisionAreAppliedOnlyAfterSuccessfulWrite(){
  var store=mock(ProfileSectionsStore.class);var service=new ProfileSectionsService(store,transactions,clock);var id=UUID.randomUUID();
  var normalized=new Skill(id,"Java engineer",4);when(store.save("owner",normalized)).thenReturn(true);
  assertEquals(new Skill(id,"Java engineer",5),service.save("owner",new Skill(id,"  Java   engineer  ",4)));
  verify(store).save("owner",normalized);
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.save("other",normalized)).reason());
 }
 @Test void deletePreservesOwnerAndVersionAndHidesMissingOrForeignEntries(){
  var store=mock(ProfileSectionsStore.class);var service=new ProfileSectionsService(store,transactions,clock);var id=UUID.randomUUID();
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.delete("owner",Section.LANGUAGE,id,7)).reason());
  verify(store).delete("owner",Section.LANGUAGE,id,7);
 }
 @Test void profileCreationUsesProviderFallbackAndUpdateNeverCreatesMissingOwner(){
  var store=mock(ProfileStore.class);var service=new ProfileService(store,transactions);
  var profile=new Profile("Member","","","",0);when(store.get("owner")).thenReturn(profile);
  assertEquals(profile,service.me("owner",null));verify(store).initialize("owner","New member");
  assertEquals(UseCaseFailure.Reason.CONFLICT,assertThrows(UseCaseFailure.class,()->service.update("missing",profile)).reason());
  verify(store,never()).initialize(eq("missing"),any());verify(store,never()).get("missing");
 }
}

