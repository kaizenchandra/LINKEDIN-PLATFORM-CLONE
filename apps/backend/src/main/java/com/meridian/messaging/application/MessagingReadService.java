package com.meridian.messaging.application;
import java.util.*;
import com.meridian.messaging.domain.Messaging.*;
import com.meridian.shared.application.UseCaseFailure;
import static com.meridian.shared.application.UseCaseFailure.Reason.INVALID;
public final class MessagingReadService implements MessagingReadUseCases {
 private final MessagingReadStore store;
 public MessagingReadService(MessagingReadStore store){this.store=store;}
 private void cursor(Long before){if(before!=null&&before<1)throw new UseCaseFailure(INVALID,"Invalid cursor");}
 public Conversations list(String subject,Long before,View view,String query){
  cursor(before);query=query.strip();if(query.length()>120)throw new UseCaseFailure(INVALID,"Search names using at most 120 characters");
  var rows=store.list(subject,before,view,query);return new Conversations(rows.stream().limit(20).map(ConversationRow::value).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null);
 }
 public Messages history(String subject,UUID id,Long before){
  cursor(before);var access=store.access(id,subject);var rows=store.messages(id,subject,before,access);
  var items=new ArrayList<>(rows.stream().limit(50).toList());Collections.reverse(items);var person=store.person(access.other(subject));
  return new Messages(items,rows.size()>50?Long.toString(rows.get(49).sequence()):null,access.latest(),access.recipientRead(subject),store.connected(access.a(),access.b()),person.memberId(),person.name(),access.preferences(subject));
 }
}
