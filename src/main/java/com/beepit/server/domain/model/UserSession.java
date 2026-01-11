package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record UserSession(
    String userId,
    String username,
    String sessionId,
    Long connectedAt
) {
    public UserSession(String userId, String username, String sessionId) {
        this(userId, username, sessionId, Instant.now().toEpochMilli());
    }
}
