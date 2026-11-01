package com.meridian.content.application;
import com.meridian.content.domain.Content.*;
import java.util.*;
public interface CommentUseCases {
 CommentPage list(String subject,UUID postId,Long before,UUID parentId);
 Comment create(String subject,UUID postId,UUID id,String text,UUID parentId);
 void delete(String subject,UUID postId,UUID id);
}
