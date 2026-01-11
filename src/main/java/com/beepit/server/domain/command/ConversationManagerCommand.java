package com.beepit.server.domain.command;

import akka.actor.typed.ActorRef;
import com.beepit.server.domain.response.ConversationManagerResponse;

public sealed interface ConversationManagerCommand 
    permits ConversationManagerCommand.SendPrivateMessage,
            ConversationManagerCommand.GetConversation,
            ConversationManagerCommand.GetUserConversations,
            ConversationManagerCommand.MarkMessageDelivered,
            ConversationManagerCommand.MarkMessageRead {
    
    public record SendPrivateMessage(String senderId, String recipientId, String content, ActorRef<ConversationManagerResponse> replyTo) implements ConversationManagerCommand {}
    public record GetConversation(String userId1, String userId2, ActorRef<ConversationManagerResponse> replyTo) implements ConversationManagerCommand {}
    public record GetUserConversations(String userId, ActorRef<ConversationManagerResponse> replyTo) implements ConversationManagerCommand {}
    public record MarkMessageDelivered(String messageId, ActorRef<ConversationManagerResponse> replyTo) implements ConversationManagerCommand {}
    public record MarkMessageRead(String messageId, ActorRef<ConversationManagerResponse> replyTo) implements ConversationManagerCommand {}
}
