package com.meridian.notifications.adapter;
import java.util.UUID;
public record ConnectionAccepted(UUID eventId,int schemaVersion,String type,UUID connectionId){
 public static final String TOPIC="meridian.connections.v1",DLQ="meridian.connections.v1.dlq";
 public void validate(){if(eventId==null||connectionId==null||schemaVersion!=1||!"ConnectionAccepted".equals(type))throw new IllegalArgumentException("Unsupported event envelope");}
}
