package com.meridian.applications.application;
import java.util.*;
import com.meridian.applications.domain.Applications.*;
import com.meridian.shared.application.UseCaseFailure;
import static com.meridian.shared.application.UseCaseFailure.Reason.NOT_FOUND;
public final class ApplicationReadService implements ApplicationReadUseCases {
 private final ApplicationReadStore store;
 public ApplicationReadService(ApplicationReadStore store){this.store=store;}
 public Page list(String subject,View view,UUID job,State status,UUID after){var rows=store.list(subject,view,job,status,after);return new Page(rows.stream().limit(20).toList(),rows.size()>20?rows.get(19).id().toString():null);}
 public History history(String subject,UUID id){var rows=store.history(subject,id);if(rows.isEmpty())throw new UseCaseFailure(NOT_FOUND,"Application unavailable");return new History(id,rows.getFirst().currentVersion(),rows.stream().map(HistoryRow::transition).toList());}
 public String resume(String subject,UUID id){return store.resume(subject,id).orElseThrow(()->new UseCaseFailure(NOT_FOUND,"Resume unavailable"));}
}
