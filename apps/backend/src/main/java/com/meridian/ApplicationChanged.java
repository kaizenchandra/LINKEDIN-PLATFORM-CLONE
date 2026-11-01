package com.meridian;
import java.util.UUID;
record ApplicationChanged(UUID eventId,int schemaVersion,String type,UUID applicationId,long applicationVersion){
 static final String TOPIC="meridian.applications.v1",DLQ="meridian.applications.v1.dlq";
 void validate(){if(eventId==null||applicationId==null||applicationVersion<1||schemaVersion!=1||!"ApplicationChanged".equals(type))throw new IllegalArgumentException("Unsupported event envelope");}
}
