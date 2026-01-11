package com.beepit.server.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import com.beepit.server.domain.command.ChatRoomCommand;
import com.beepit.server.domain.command.UserManagerCommand;
import com.beepit.server.domain.command.ConversationManagerCommand;
import jakarta.inject.Singleton;
import jakarta.annotation.PreDestroy;

/**
 * Provider for Akka Actor Systems.
 * 
 * Uses 3 separate ActorSystems for type safety - this is the correct pattern
 * for Akka Typed when actors have incompatible message types.
 * 
 * Each ActorSystem is itself the guardian actor, and in Akka Typed you cannot
 * spawn child actors of different types from the ActorSystem directly.
 * The alternative would be a complex dispatcher actor, but separate systems
 * provides better isolation and simpler code.
 */
@Singleton
public class ActorSystemProvider {
    
    private final ActorSystem<ChatRoomCommand> chatRoomSystem;
    private final ActorSystem<UserManagerCommand> userManagerSystem;
    private final ActorSystem<ConversationManagerCommand> conversationManagerSystem;
    
    public ActorSystemProvider() {
        this.chatRoomSystem = ActorSystem.create(
            ChatRoomActor.create(), 
            "chat-room-system"
        );
        this.userManagerSystem = ActorSystem.create(
            UserManagerActor.create(), 
            "user-manager-system"
        );
        this.conversationManagerSystem = ActorSystem.create(
            ConversationManagerActor.create(), 
            "conversation-manager-system"
        );
    }
    
    public ActorRef<ChatRoomCommand> getChatRoomActor() {
        return chatRoomSystem;
    }
    
    public ActorRef<UserManagerCommand> getUserManagerActor() {
        return userManagerSystem;
    }
    
    public ActorRef<ConversationManagerCommand> getConversationManagerActor() {
        return conversationManagerSystem;
    }
    
    /**
     * Returns a shared scheduler instance. All ActorSystems share the same
     * underlying scheduler, so we can use any of them.
     */
    public akka.actor.typed.Scheduler getScheduler() {
        return userManagerSystem.scheduler();
    }
    
    @PreDestroy
    public void shutdown() {
        chatRoomSystem.terminate();
        userManagerSystem.terminate();
        conversationManagerSystem.terminate();
    }
}



