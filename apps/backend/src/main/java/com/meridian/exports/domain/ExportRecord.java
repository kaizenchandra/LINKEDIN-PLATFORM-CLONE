package com.meridian.exports.domain;
import java.time.Instant;
import java.util.UUID;
public record ExportRecord(UUID id,String status,Instant createdAt,Instant expiresAt,String failure){
 public ExportRecord at(Instant now){return "READY".equals(status)&&expiresAt.isBefore(now)?new ExportRecord(id,"EXPIRED",createdAt,expiresAt,failure):this;}
}
