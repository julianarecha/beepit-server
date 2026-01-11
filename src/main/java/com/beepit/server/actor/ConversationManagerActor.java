package com.beepit.server.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.beepit.server.domain.command.ConversationManagerCommand;
import com.beepit.server.domain.command.ConversationManagerCommand.*;
import com.beepit.server.domain.response.ConversationManagerResponse;
import com.beepit.server.domain.response.ConversationManagerResponse.*;
import com.beepit.server.domain.model.Conversation;
import com.beepit.server.domain.model.PrivateMessage;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationManagerActor extends AbstractBehavior<ConversationManagerCommand> {

    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final Map<String, PrivateMessage> messagesById = new ConcurrentHashMap<>();

    private ConversationManagerActor(ActorContext<ConversationManagerCommand> context) {
        super(context);
    }

    public static Behavior<ConversationManagerCommand> create() {
        return Behaviors.setup(ConversationManagerActor::new);
    }

    @Override
    public Receive<ConversationManagerCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SendPrivateMessage.class, this::onSendPrivateMessage)
                .onMessage(GetConversation.class, this::onGetConversation)
                .onMessage(GetUserConversations.class, this::onGetUserConversations)
                .onMessage(MarkMessageDelivered.class, this::onMarkMessageDelivered)
                .onMessage(MarkMessageRead.class, this::onMarkMessageRead)
                .build();
    }

    private Behavior<ConversationManagerCommand> onSendPrivateMessage(SendPrivateMessage cmd) {
        PrivateMessage message = new PrivateMessage(cmd.senderId(), cmd.recipientId(), cmd.content());
        messagesById.put(message.messageId(), message);
        
        String conversationId = getConversationId(cmd.senderId(), cmd.recipientId());
        Conversation conversation = conversations.get(conversationId);
        
        if (conversation == null) {
            List<String> participants = Arrays.asList(cmd.senderId(), cmd.recipientId());
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
        cmd.replyTo().tell(new MessageSent(message));
        return this;
    }

    private Behavior<ConversationManagerCommand> onGetConversation(GetConversation cmd) {
        String conversationId = getConversationId(cmd.userId1(), cmd.userId2());
        Conversation conversation = conversations.get(conversationId);
        
        if (conversation != null) {
            cmd.replyTo().tell(new ConversationFound(conversation));
        } else {
            // Crear conversación vacía
            List<String> participants = Arrays.asList(cmd.userId1(), cmd.userId2());
            Conversation emptyConversation = new Conversation(conversationId, participants);
            conversations.put(conversationId, emptyConversation);
            cmd.replyTo().tell(new ConversationFound(emptyConversation));
        }
        return this;
    }

    private Behavior<ConversationManagerCommand> onGetUserConversations(GetUserConversations cmd) {
        List<Conversation> userConversations = conversations.values().stream()
                .filter(conv -> conv.participants().contains(cmd.userId()))
                .sorted((c1, c2) -> c2.lastMessageAt().compareTo(c1.lastMessageAt()))
                .toList();
        
        cmd.replyTo().tell(new ConversationsList(userConversations));
        return this;
    }

    private Behavior<ConversationManagerCommand> onMarkMessageDelivered(MarkMessageDelivered cmd) {
        PrivateMessage message = messagesById.get(cmd.messageId());
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
            messagesById.put(cmd.messageId(), updated);
            updateMessageInConversations(updated);
            cmd.replyTo().tell(new MessageUpdated(cmd.messageId()));
        } else {
            cmd.replyTo().tell(new ErrorResponse("Message not found"));
        }
        return this;
    }

    private Behavior<ConversationManagerCommand> onMarkMessageRead(MarkMessageRead cmd) {
        PrivateMessage message = messagesById.get(cmd.messageId());
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
            messagesById.put(cmd.messageId(), updated);
            updateMessageInConversations(updated);
            cmd.replyTo().tell(new MessageUpdated(cmd.messageId()));
        } else {
            cmd.replyTo().tell(new ErrorResponse("Message not found"));
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
