package com.meridian;
import java.util.UUID;
record ConnectionAccepted(UUID eventId,int schemaVersion,String type,UUID connectionId){
 static final String TOPIC="meridian.connections.v1",DLQ="meridian.connections.v1.dlq";
 void validate(){if(eventId==null||connectionId==null||schemaVersion!=1||!"ConnectionAccepted".equals(type))throw new IllegalArgumentException("Unsupported event envelope");}
}
