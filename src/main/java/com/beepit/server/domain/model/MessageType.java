package com.beepit.server.domain.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}
