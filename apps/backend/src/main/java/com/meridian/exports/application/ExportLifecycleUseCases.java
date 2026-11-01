package com.meridian.exports.application;
import java.util.*;
import com.meridian.exports.domain.ExportRecord;
public interface ExportLifecycleUseCases {
 record Requested(ExportRecord export,boolean throttled){}
 List<ExportRecord> list(String subject);Requested request(String subject,UUID id);byte[] download(String subject,UUID id);void cancel(String subject,UUID id);
}
