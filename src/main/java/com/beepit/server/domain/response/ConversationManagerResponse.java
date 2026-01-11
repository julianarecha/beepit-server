package com.beepit.server.domain.response;

import com.beepit.server.domain.model.Conversation;
import com.beepit.server.domain.model.PrivateMessage;

import java.util.List;

public sealed interface ConversationManagerResponse 
    permits ConversationManagerResponse.MessageSent,
            ConversationManagerResponse.ConversationFound,
            ConversationManagerResponse.ConversationsList,
            ConversationManagerResponse.MessageUpdated,
            ConversationManagerResponse.ErrorResponse {
    
    public record MessageSent(PrivateMessage message) implements ConversationManagerResponse {}
    public record ConversationFound(Conversation conversation) implements ConversationManagerResponse {}
    public record ConversationsList(List<Conversation> conversations) implements ConversationManagerResponse {}
    public record MessageUpdated(String messageId) implements ConversationManagerResponse {}
    public record ErrorResponse(String message) implements ConversationManagerResponse {}
}
