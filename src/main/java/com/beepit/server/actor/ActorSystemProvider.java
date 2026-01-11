package com.beepit.server.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import jakarta.inject.Singleton;
import jakarta.annotation.PreDestroy;

@Singleton
public class ActorSystemProvider {
    
    private final ActorSystem<ChatRoomActor.Command> chatRoomSystem;
    private final ActorSystem<UserManagerActor.Command> userManagerSystem;
    private final ActorSystem<ConversationManagerActor.Command> conversationManagerSystem;
    
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
    
    public ActorSystem<ChatRoomActor.Command> getChatRoomSystem() {
        return chatRoomSystem;
    }
    
    public ActorRef<ChatRoomActor.Command> getChatRoomActor() {
        return chatRoomSystem;
    }
    
    public ActorRef<UserManagerActor.Command> getUserManagerActor() {
        return userManagerSystem;
    }
    
    public ActorRef<ConversationManagerActor.Command> getConversationManagerActor() {
        return conversationManagerSystem;
    }
    
    public akka.actor.typed.Scheduler getChatRoomScheduler() {
        return chatRoomSystem.scheduler();
    }
    
    public akka.actor.typed.Scheduler getUserManagerScheduler() {
        return userManagerSystem.scheduler();
    }
    
    public akka.actor.typed.Scheduler getConversationManagerScheduler() {
        return conversationManagerSystem.scheduler();
    }
    
    @PreDestroy
    public void shutdown() {
        chatRoomSystem.terminate();
        userManagerSystem.terminate();
        conversationManagerSystem.terminate();
    }
}



