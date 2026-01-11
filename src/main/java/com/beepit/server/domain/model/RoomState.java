package com.beepit.server.domain.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomState {
    private final Map<String, UserSession> participants = new ConcurrentHashMap<>();
    private final List<Message> messageHistory = Collections.synchronizedList(new ArrayList<>());
    
    public Map<String, UserSession> getParticipants() {
        return participants;
    }
    
    public List<Message> getMessageHistory() {
        return messageHistory;
    }
}
