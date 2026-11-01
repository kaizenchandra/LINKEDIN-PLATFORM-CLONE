package com.meridian.exports.application;
import java.util.*;
import com.meridian.exports.domain.ExportRecord;
public interface ExportLifecycleStore {
 List<ExportRecord> list(String subject);Optional<ExportRecord> owned(String subject,UUID id);
 boolean lockProfile(String subject);boolean pending(String subject);boolean recent(String subject);boolean insert(String subject,UUID id);
 Optional<byte[]> lockPayload(String subject,UUID id);boolean cancel(String subject,UUID id);void audit(UUID id,String action);
}
