package com.beepit.server.domain.response;

import com.beepit.server.domain.model.UserSession;

import java.util.Set;

public record ParticipantsResponse(Set<UserSession> participants) {}
