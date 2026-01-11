package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.List;

@Serdeable
public record Conversation(
    String conversationId,
    List<String> participants,
    List<PrivateMessage> messages,
    Instant createdAt,
    Instant lastMessageAt
) {
    public Conversation(String conversationId, List<String> participants) {
        this(conversationId, participants, List.of(), Instant.now(), Instant.now());
    }
}
