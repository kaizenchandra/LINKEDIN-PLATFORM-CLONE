package com.meridian.community.application;
import com.meridian.community.domain.Community.*;
import java.util.*;
public interface CommunityStore {
 List<Update> catchUp(String subject);
 List<Entry> list(String subject,Kind kind);
 boolean profileExists(String subject);
 boolean create(String subject,Draft draft);
 boolean exists(UUID id);
 void membership(String subject,UUID id,boolean joined);
}
