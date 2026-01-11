package com.beepit.server.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.beepit.server.domain.model.Conversation;
import com.beepit.server.domain.model.PrivateMessage;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManagerActor extends AbstractBehavior<ConversationManagerActor.Command> {

    public sealed interface Command {}
    
    public sealed interface Response {}
    
    // Commands
    public record SendPrivateMessage(String senderId, String recipientId, String content, ActorRef<Response> replyTo) implements Command {}
    public record GetConversation(String userId1, String userId2, ActorRef<Response> replyTo) implements Command {}
    public record GetUserConversations(String userId, ActorRef<Response> replyTo) implements Command {}
    public record MarkMessageDelivered(String messageId, ActorRef<Response> replyTo) implements Command {}
    public record MarkMessageRead(String messageId, ActorRef<Response> replyTo) implements Command {}
    
    // Responses
    public record MessageSent(PrivateMessage message) implements Response {}
    public record ConversationFound(Conversation conversation) implements Response {}
    public record ConversationsList(List<Conversation> conversations) implements Response {}
    public record MessageUpdated(String messageId) implements Response {}
    public record ErrorResponse(String message) implements Response {}

    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, PrivateMessage> messagesById = new ConcurrentHashMap<>();

    private ConversationManagerActor(ActorContext<Command> context) {
        super(context);
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ConversationManagerActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(SendPrivateMessage.class, this::onSendPrivateMessage)
                .onMessage(GetConversation.class, this::onGetConversation)
                .onMessage(GetUserConversations.class, this::onGetUserConversations)
                .onMessage(MarkMessageDelivered.class, this::onMarkMessageDelivered)
                .onMessage(MarkMessageRead.class, this::onMarkMessageRead)
                .build();
    }

    private Behavior<Command> onSendPrivateMessage(SendPrivateMessage cmd) {
        PrivateMessage message = new PrivateMessage(cmd.senderId, cmd.recipientId, cmd.content);
        messagesById.put(message.messageId(), message);
        
        String conversationId = getConversationId(cmd.senderId, cmd.recipientId);
        Conversation conversation = conversations.get(conversationId);
        
        if (conversation == null) {
            List<String> participants = Arrays.asList(cmd.senderId, cmd.recipientId);
            List<PrivateMessage> messages = new ArrayList<>();
            messages.add(message);
            conversation = new Conversation(
                conversationId,
                participants,
                messages,
                Instant.now(),
                Instant.now()
            );
        } else {
            List<PrivateMessage> messages = new ArrayList<>(conversation.messages());
            messages.add(message);
            conversation = new Conversation(
                conversation.conversationId(),
                conversation.participants(),
                messages,
                conversation.createdAt(),
                Instant.now()
            );
        }
        
        conversations.put(conversationId, conversation);
        cmd.replyTo.tell(new MessageSent(message));
        return this;
    }

    private Behavior<Command> onGetConversation(GetConversation cmd) {
        String conversationId = getConversationId(cmd.userId1, cmd.userId2);
        Conversation conversation = conversations.get(conversationId);
        
        if (conversation != null) {
            cmd.replyTo.tell(new ConversationFound(conversation));
        } else {
            // Crear conversación vacía
            List<String> participants = Arrays.asList(cmd.userId1, cmd.userId2);
            Conversation emptyConversation = new Conversation(conversationId, participants);
            conversations.put(conversationId, emptyConversation);
            cmd.replyTo.tell(new ConversationFound(emptyConversation));
        }
        return this;
    }

    private Behavior<Command> onGetUserConversations(GetUserConversations cmd) {
        List<Conversation> userConversations = conversations.values().stream()
                .filter(conv -> conv.participants().contains(cmd.userId))
                .sorted((c1, c2) -> c2.lastMessageAt().compareTo(c1.lastMessageAt()))
                .toList();
        
        cmd.replyTo.tell(new ConversationsList(userConversations));
        return this;
    }

    private Behavior<Command> onMarkMessageDelivered(MarkMessageDelivered cmd) {
        PrivateMessage message = messagesById.get(cmd.messageId);
        if (message != null) {
            PrivateMessage updated = new PrivateMessage(
                message.messageId(),
                message.senderId(),
                message.recipientId(),
                message.content(),
                message.timestamp(),
                true,
                message.read()
            );
            messagesById.put(cmd.messageId, updated);
            updateMessageInConversations(updated);
            cmd.replyTo.tell(new MessageUpdated(cmd.messageId));
        } else {
            cmd.replyTo.tell(new ErrorResponse("Message not found"));
        }
        return this;
    }

    private Behavior<Command> onMarkMessageRead(MarkMessageRead cmd) {
        PrivateMessage message = messagesById.get(cmd.messageId);
        if (message != null) {
            PrivateMessage updated = new PrivateMessage(
                message.messageId(),
                message.senderId(),
                message.recipientId(),
                message.content(),
                message.timestamp(),
                true,
                true
            );
            messagesById.put(cmd.messageId, updated);
            updateMessageInConversations(updated);
            cmd.replyTo.tell(new MessageUpdated(cmd.messageId));
        } else {
            cmd.replyTo.tell(new ErrorResponse("Message not found"));
        }
        return this;
    }

    private void updateMessageInConversations(PrivateMessage updatedMessage) {
        String conversationId = getConversationId(updatedMessage.senderId(), updatedMessage.recipientId());
        Conversation conversation = conversations.get(conversationId);
        
        if (conversation != null) {
            List<PrivateMessage> updatedMessages = conversation.messages().stream()
                    .map(msg -> msg.messageId().equals(updatedMessage.messageId()) ? updatedMessage : msg)
                    .toList();
            
            Conversation updatedConversation = new Conversation(
                conversation.conversationId(),
                conversation.participants(),
                updatedMessages,
                conversation.createdAt(),
                conversation.lastMessageAt()
            );
            conversations.put(conversationId, updatedConversation);
        }
    }

    private String getConversationId(String userId1, String userId2) {
        List<String> sorted = Arrays.asList(userId1, userId2);
        Collections.sort(sorted);
        return String.join("_", sorted);
    }
}
