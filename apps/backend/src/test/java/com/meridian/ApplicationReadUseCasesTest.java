package com.meridian;
import java.util.*;
import com.meridian.applications.application.*;
import com.meridian.applications.domain.Applications.*;
import com.meridian.applications.domain.Applications.Application;
import com.meridian.shared.application.UseCaseFailure;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ApplicationReadUseCasesTest {
 private final ApplicationReadStore store=mock(ApplicationReadStore.class);
 private final ApplicationReadService service=new ApplicationReadService(store);
 @Test void missingAndUnauthorizedPrivateResourcesHaveUnavailableOutcome(){
  var id=UUID.randomUUID();when(store.resume("outsider",id)).thenReturn(Optional.empty());when(store.history("outsider",id)).thenReturn(List.of());
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.resume("outsider",id)).reason());
  assertEquals(UseCaseFailure.Reason.NOT_FOUND,assertThrows(UseCaseFailure.class,()->service.history("outsider",id)).reason());
 }
 @Test void paginationUsesLastVisibleItemAndRetainsCallerFilters(){
  var job=UUID.randomUUID();var after=UUID.randomUUID();var rows=new ArrayList<Application>();
  for(int i=0;i<21;i++)rows.add(new Application(UUID.randomUUID(),job,"Role","Company","Name","Headline","Statement",State.SUBMITTED,1,true,"2026-01-01T00:00:00Z",false,0,List.of()));
  when(store.list("member",View.RECEIVED,job,State.SUBMITTED,after)).thenReturn(rows);
  var page=service.list("member",View.RECEIVED,job,State.SUBMITTED,after);assertEquals(20,page.items().size());assertEquals(rows.get(19).id().toString(),page.nextCursor());
  when(store.list("member",View.RECEIVED,job,State.SUBMITTED,after)).thenReturn(rows.subList(0,20));assertNull(service.list("member",View.RECEIVED,job,State.SUBMITTED,after).nextCursor());
 }
 @Test void historyRetainsAuthoritativeVersionAndActorRoles(){
  var id=UUID.randomUUID();var transition=new Transition(1,"APPLICANT",null,"SUBMITTED","2026-01-01T00:00:00Z");when(store.history("member",id)).thenReturn(List.of(new HistoryRow(3,transition)));
  assertEquals(new History(id,3,List.of(transition)),service.history("member",id));
  when(store.resume("member",id)).thenReturn(Optional.of("Résumé\nExperience"));assertEquals("Résumé\nExperience",service.resume("member",id));
 }
}
