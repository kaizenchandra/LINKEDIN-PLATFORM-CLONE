package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
public interface CommentStore {
 boolean parentVisible(String subject,UUID postId,UUID parentId);
 List<CommentRow> list(String subject,UUID postId,Long before,UUID parentId);
 void insert(String subject,UUID postId,UUID id,String text,UUID parentId);
 Optional<Comment> matching(String subject,UUID postId,UUID id,String text,UUID parentId);
 boolean delete(String subject,UUID postId,UUID id);
}
