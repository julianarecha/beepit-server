package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.UUID;

@Serdeable
public record User(
    String id,
    String username,
    String email,
    UserStatus status,
    Long createdAt
) {
    public User(String username, String email) {
        this(
            UUID.randomUUID().toString(),
            username,
            email,
            UserStatus.OFFLINE,
            Instant.now().toEpochMilli()
        );
    }
}

@Serdeable
enum UserStatus {
    ONLINE,
    AWAY,
    BUSY,
    OFFLINE
}
