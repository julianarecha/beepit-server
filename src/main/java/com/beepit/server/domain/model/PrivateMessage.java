package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public record PrivateMessage(
    String messageId,
    String senderId,
    String recipientId,
    String content,
    Instant timestamp,
    boolean delivered,
    boolean read
) {
    public PrivateMessage(String senderId, String recipientId, String content) {
        this(
            java.util.UUID.randomUUID().toString(),
            senderId,
            recipientId,
            content,
            Instant.now(),
            false,
            false
        );
    }
}
