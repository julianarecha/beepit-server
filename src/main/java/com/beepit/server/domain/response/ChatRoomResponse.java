package com.beepit.server.domain.response;

import java.util.Set;

public sealed interface ChatRoomResponse 
    permits ChatRoomResponse.JoinedRoom,
            ChatRoomResponse.MessageSent,
            ChatRoomResponse.ErrorResponse {
    
    public record JoinedRoom(String roomId, Set<String> participants) implements ChatRoomResponse {}
    public record MessageSent(String messageId) implements ChatRoomResponse {}
    public record ErrorResponse(String message) implements ChatRoomResponse {}
}
