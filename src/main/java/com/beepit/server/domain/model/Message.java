package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.UUID;

@Serdeable
public record Message(
    String id,
    String sender,
    String content,
    Long timestamp,
    String roomId,
    MessageType type
) {
    public Message(String sender, String content, String roomId) {
        this(
            UUID.randomUUID().toString(),
            sender,
            content,
            Instant.now().toEpochMilli(),
            roomId,
            MessageType.TEXT
        );
    }
}

@Serdeable
enum MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}
