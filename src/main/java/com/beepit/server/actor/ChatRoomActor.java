package com.beepit.server.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.beepit.server.domain.model.Message;
import com.beepit.server.domain.model.UserSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomActor extends AbstractBehavior<ChatRoomActor.Command> {
    
    // Commands
    public sealed interface Command permits JoinRoom, LeaveRoom, SendMessage, GetRoomParticipants {}
    
    public record JoinRoom(
        UserSession session,
        String roomId,
        ActorRef<Response> replyTo
    ) implements Command {}
    
    public record LeaveRoom(
        String userId,
        String roomId
    ) implements Command {}
    
    public record SendMessage(
        Message message,
        ActorRef<Response> replyTo
    ) implements Command {}
    
    public record GetRoomParticipants(
        String roomId,
        ActorRef<ParticipantsResponse> replyTo
    ) implements Command {}
    
    // Responses
    public sealed interface Response permits JoinedRoom, MessageSent, ErrorResponse {}
    
    public record JoinedRoom(String roomId, Set<String> participants) implements Response {}
    public record MessageSent(String messageId) implements Response {}
    public record ErrorResponse(String message) implements Response {}
    public record ParticipantsResponse(Set<UserSession> participants) {}
    
    // State
    public static class RoomState {
        private final Map<String, UserSession> participants = new ConcurrentHashMap<>();
        private final List<Message> messageHistory = Collections.synchronizedList(new ArrayList<>());
        
        public Map<String, UserSession> getParticipants() {
            return participants;
        }
        
        public List<Message> getMessageHistory() {
            return messageHistory;
        }
    }
    
    private final Map<String, RoomState> rooms;
    
    private ChatRoomActor(ActorContext<Command> context, Map<String, RoomState> rooms) {
        super(context);
        this.rooms = rooms;
    }
    
    public static Behavior<Command> create() {
        return Behaviors.setup(context -> 
            new ChatRoomActor(context, new ConcurrentHashMap<>())
        );
    }
    
    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(JoinRoom.class, this::onJoinRoom)
            .onMessage(LeaveRoom.class, this::onLeaveRoom)
            .onMessage(SendMessage.class, this::onSendMessage)
            .onMessage(GetRoomParticipants.class, this::onGetParticipants)
            .build();
    }
    
    private Behavior<Command> onJoinRoom(JoinRoom command) {
        getContext().getLog().info("User {} joining room {}", 
            command.session.username(), command.roomId);
        
        RoomState room = rooms.computeIfAbsent(command.roomId, k -> new RoomState());
        room.getParticipants().put(command.session.userId(), command.session);
        
        command.replyTo.tell(new JoinedRoom(
            command.roomId, 
            room.getParticipants().keySet()
        ));
        
        return this;
    }
    
    private Behavior<Command> onLeaveRoom(LeaveRoom command) {
        getContext().getLog().info("User {} leaving room {}", 
            command.userId, command.roomId);
        
        RoomState room = rooms.get(command.roomId);
        if (room != null) {
            room.getParticipants().remove(command.userId);
        }
        
        return this;
    }
    
    private Behavior<Command> onSendMessage(SendMessage command) {
        getContext().getLog().info("Message from {} in room {}", 
            command.message.sender(), command.message.roomId());
        
        RoomState room = rooms.get(command.message.roomId());
        if (room != null) {
            room.getMessageHistory().add(command.message);
            command.replyTo.tell(new MessageSent(command.message.id()));
        } else {
            command.replyTo.tell(new ErrorResponse("Room not found: " + command.message.roomId()));
        }
        
        return this;
    }
    
    private Behavior<Command> onGetParticipants(GetRoomParticipants command) {
        RoomState room = rooms.get(command.roomId);
        Set<UserSession> participants = room != null 
            ? new HashSet<>(room.getParticipants().values())
            : Collections.emptySet();
        
        command.replyTo.tell(new ParticipantsResponse(participants));
        
        return this;
    }
}
