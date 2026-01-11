package com.beepit.server.actor;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.beepit.server.domain.command.ChatRoomCommand;
import com.beepit.server.domain.command.ChatRoomCommand.*;
import com.beepit.server.domain.response.ChatRoomResponse;
import com.beepit.server.domain.response.ChatRoomResponse.*;
import com.beepit.server.domain.response.ParticipantsResponse;
import com.beepit.server.domain.model.RoomState;
import com.beepit.server.domain.model.UserSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomActor extends AbstractBehavior<ChatRoomCommand> {
    
    private final Map<String, RoomState> rooms;
    
    private ChatRoomActor(ActorContext<ChatRoomCommand> context, Map<String, RoomState> rooms) {
        super(context);
        this.rooms = rooms;
    }
    
    public static Behavior<ChatRoomCommand> create() {
        return Behaviors.setup(context -> 
            new ChatRoomActor(context, new ConcurrentHashMap<>())
        );
    }
    
    @Override
    public Receive<ChatRoomCommand> createReceive() {
        return newReceiveBuilder()
            .onMessage(JoinRoom.class, this::onJoinRoom)
            .onMessage(LeaveRoom.class, this::onLeaveRoom)
            .onMessage(SendMessage.class, this::onSendMessage)
            .onMessage(GetRoomParticipants.class, this::onGetParticipants)
            .build();
    }
    
    private Behavior<ChatRoomCommand> onJoinRoom(JoinRoom command) {
        getContext().getLog().info("User {} joining room {}", 
            command.session().username(), command.roomId());
        
        RoomState room = rooms.computeIfAbsent(command.roomId(), k -> new RoomState());
        room.getParticipants().put(command.session().userId(), command.session());
        
        command.replyTo().tell(new JoinedRoom(
            command.roomId(), 
            room.getParticipants().keySet()
        ));
        
        return this;
    }
    
    private Behavior<ChatRoomCommand> onLeaveRoom(LeaveRoom command) {
        getContext().getLog().info("User {} leaving room {}", 
            command.userId(), command.roomId());
        
        RoomState room = rooms.get(command.roomId());
        if (room != null) {
            room.getParticipants().remove(command.userId());
        }
        
        return this;
    }
    
    private Behavior<ChatRoomCommand> onSendMessage(SendMessage command) {
        getContext().getLog().info("Message from {} in room {}", 
            command.message().sender(), command.message().roomId());
        
        RoomState room = rooms.get(command.message().roomId());
        if (room != null) {
            room.getMessageHistory().add(command.message());
            command.replyTo().tell(new MessageSent(command.message().id()));
        } else {
            command.replyTo().tell(new ErrorResponse("Room not found: " + command.message().roomId()));
        }
        
        return this;
    }
    
    private Behavior<ChatRoomCommand> onGetParticipants(GetRoomParticipants command) {
        RoomState room = rooms.get(command.roomId());
        Set<UserSession> participants = room != null 
            ? new HashSet<>(room.getParticipants().values())
            : Collections.emptySet();
        
        command.replyTo().tell(new ParticipantsResponse(participants));
        
        return this;
    }
}
