package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record Contact(
    String userId,
    String username,
    String status,
    boolean online
) {
    public Contact(String userId, String username) {
        this(userId, username, "Hey there! I'm using Beepit", false);
    }
}
