package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Serdeable
public record AppUser(
    String userId,
    String username,
    String password,
    List<String> contactIds,
    Instant createdAt,
    boolean online
) {
    public AppUser(String username, String password) {
        this(
            java.util.UUID.randomUUID().toString(),
            username,
            password,
            new ArrayList<>(),
            Instant.now(),
            false
        );
    }
}
