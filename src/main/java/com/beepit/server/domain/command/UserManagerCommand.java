package com.beepit.server.domain.command;

import akka.actor.typed.ActorRef;
import com.beepit.server.domain.response.UserManagerResponse;

public sealed interface UserManagerCommand 
    permits UserManagerCommand.RegisterUser,
            UserManagerCommand.LoginUser,
            UserManagerCommand.GetUser,
            UserManagerCommand.GetAllUsers,
            UserManagerCommand.AddContact,
            UserManagerCommand.GetContacts,
            UserManagerCommand.SetUserOnline {
    
    public record RegisterUser(String username, String password, ActorRef<UserManagerResponse> replyTo) implements UserManagerCommand {}
    public record LoginUser(String username, String password, ActorRef<UserManagerResponse> replyTo) implements UserManagerCommand {}
    public record GetUser(String userId, ActorRef<UserManagerResponse> replyTo) implements UserManagerCommand {}
    public record GetAllUsers(ActorRef<UserManagerResponse> replyTo) implements UserManagerCommand {}
    public record AddContact(String userId, String contactId, ActorRef<UserManagerResponse> replyTo) implements UserManagerCommand {}
    public record GetContacts(String userId, ActorRef<UserManagerResponse> replyTo) implements UserManagerCommand {}
    public record SetUserOnline(String userId, boolean online) implements UserManagerCommand {}
}
