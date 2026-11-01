package com.meridian.events.domain;
import java.util.UUID;
public record OutboxEvent(UUID id,UUID connectionId,UUID applicationId,Long applicationVersion){}
