package com.beepit.server.domain.command;

import akka.actor.typed.ActorRef;
import com.beepit.server.domain.model.Message;
import com.beepit.server.domain.model.UserSession;
import com.beepit.server.domain.response.ChatRoomResponse;
import com.beepit.server.domain.response.ParticipantsResponse;

public sealed interface ChatRoomCommand 
    permits ChatRoomCommand.JoinRoom,
            ChatRoomCommand.LeaveRoom,
            ChatRoomCommand.SendMessage,
            ChatRoomCommand.GetRoomParticipants {
    
    public record JoinRoom(UserSession session, String roomId, ActorRef<ChatRoomResponse> replyTo) implements ChatRoomCommand {}
    public record LeaveRoom(String userId, String roomId) implements ChatRoomCommand {}
    public record SendMessage(Message message, ActorRef<ChatRoomResponse> replyTo) implements ChatRoomCommand {}
    public record GetRoomParticipants(String roomId, ActorRef<ParticipantsResponse> replyTo) implements ChatRoomCommand {}
}
