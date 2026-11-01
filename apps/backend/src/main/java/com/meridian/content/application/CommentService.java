package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
import com.meridian.shared.application.*;
import static com.meridian.shared.application.UseCaseFailure.Reason.*;
public final class CommentService implements CommentUseCases {
 private final CommentStore store;private final ContentAccess access;private final Transactions transactions;
 public CommentService(CommentStore store,ContentAccess access,Transactions transactions){this.store=store;this.access=access;this.transactions=transactions;}
 private void parent(String subject,UUID postId,UUID parentId){if(parentId!=null&&!store.parentVisible(subject,postId,parentId))throw new UseCaseFailure(NOT_FOUND,"Parent comment unavailable");}
 public CommentPage list(String subject,UUID postId,Long before,UUID parentId){
  ContentFailures.cursor(before);if(!access.visible(subject,postId))throw ContentFailures.unavailable();parent(subject,postId,parentId);
  var rows=store.list(subject,postId,before,parentId);return new CommentPage(rows.stream().limit(20).map(CommentRow::comment).toList(),rows.size()>20?Long.toString(rows.get(19).sequence()):null);
 }
 public Comment create(String subject,UUID postId,UUID id,String text,UUID parentId){
  ContentFailures.text(text,2000,true);return transactions.write(()->{
   if(!access.lockVisible(subject,postId))throw ContentFailures.unavailable();String body=text.strip();parent(subject,postId,parentId);
   store.insert(subject,postId,id,body,parentId);return store.matching(subject,postId,id,body,parentId).orElseThrow(()->new UseCaseFailure(CONFLICT,"Comment unavailable or request ID reused"));
  });
 }
 public void delete(String subject,UUID postId,UUID id){transactions.write(()->{
  if(!access.lockVisible(subject,postId))throw ContentFailures.unavailable();if(!store.delete(subject,postId,id))throw new UseCaseFailure(NOT_FOUND,"Comment unavailable");return null;
 });}
}
