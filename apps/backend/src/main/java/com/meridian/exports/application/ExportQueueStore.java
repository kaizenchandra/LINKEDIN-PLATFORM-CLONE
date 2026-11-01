package com.meridian.exports.application;
import com.meridian.exports.domain.ExportWork;
public interface ExportQueueStore {
 void expire();void failExhausted();ExportWork lockNext();void lease(ExportWork work);
 boolean complete(ExportWork work,byte[] data);void fail(ExportWork work,String status,String failure);
}
