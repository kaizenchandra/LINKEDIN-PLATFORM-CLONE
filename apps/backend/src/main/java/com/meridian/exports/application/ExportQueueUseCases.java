package com.meridian.exports.application;
import com.meridian.exports.domain.ExportWork;
public interface ExportQueueUseCases {ExportWork claim();boolean complete(ExportWork work,byte[] data);void fail(ExportWork work,boolean tooLarge);}
