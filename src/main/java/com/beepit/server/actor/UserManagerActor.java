package com.beepit.server.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.beepit.server.domain.model.AppUser;
import com.beepit.server.domain.model.Contact;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserManagerActor extends AbstractBehavior<UserManagerActor.Command> {

    public sealed interface Command {}
    
    public sealed interface Response {}
    
    // Commands
    public record RegisterUser(String username, String password, ActorRef<Response> replyTo) implements Command {}
    public record LoginUser(String username, String password, ActorRef<Response> replyTo) implements Command {}
    public record GetUser(String userId, ActorRef<Response> replyTo) implements Command {}
    public record GetAllUsers(ActorRef<Response> replyTo) implements Command {}
    public record AddContact(String userId, String contactId, ActorRef<Response> replyTo) implements Command {}
    public record GetContacts(String userId, ActorRef<Response> replyTo) implements Command {}
    public record SetUserOnline(String userId, boolean online) implements Command {}
    
    // Responses
    public record UserRegistered(AppUser user) implements Response {}
    public record UserLoggedIn(AppUser user) implements Response {}
    public record UserFound(AppUser user) implements Response {}
    public record AllUsers(List<AppUser> users) implements Response {}
    public record ContactAdded(String userId, String contactId) implements Response {}
    public record ContactList(List<Contact> contacts) implements Response {}
    public record ErrorResponse(String message) implements Response {}

    private final Map<String, AppUser> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> userIdsByUsername = new ConcurrentHashMap<>();

    private UserManagerActor(ActorContext<Command> context) {
        super(context);
        // Pre-crear usuarios de prueba
        createTestUsers();
    }

    private void createTestUsers() {
        String[] testUsers = {"Alice", "Bob", "Charlie", "Diana", "Eve"};
        for (String username : testUsers) {
            AppUser user = new AppUser(username, "password123");
            usersById.put(user.userId(), user);
            userIdsByUsername.put(username.toLowerCase(), user.userId());
        }
        
        // No agregar contactos - todos empiezan sin contactos
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(UserManagerActor::new);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RegisterUser.class, this::onRegisterUser)
                .onMessage(LoginUser.class, this::onLoginUser)
                .onMessage(GetUser.class, this::onGetUser)
                .onMessage(GetAllUsers.class, this::onGetAllUsers)
                .onMessage(AddContact.class, this::onAddContact)
                .onMessage(GetContacts.class, this::onGetContacts)
                .onMessage(SetUserOnline.class, this::onSetUserOnline)
                .build();
    }

    private Behavior<Command> onRegisterUser(RegisterUser cmd) {
        String usernameLower = cmd.username.toLowerCase();
        
        if (userIdsByUsername.containsKey(usernameLower)) {
            cmd.replyTo.tell(new ErrorResponse("Username already exists"));
            return this;
        }

        AppUser newUser = new AppUser(cmd.username, cmd.password);
        usersById.put(newUser.userId(), newUser);
        userIdsByUsername.put(usernameLower, newUser.userId());
        
        cmd.replyTo.tell(new UserRegistered(newUser));
        return this;
    }

    private Behavior<Command> onLoginUser(LoginUser cmd) {
        String usernameLower = cmd.username.toLowerCase();
        String userId = userIdsByUsername.get(usernameLower);
        
        if (userId == null) {
            cmd.replyTo.tell(new ErrorResponse("User not found"));
            return this;
        }

        AppUser user = usersById.get(userId);
        if (!user.password().equals(cmd.password)) {
            cmd.replyTo.tell(new ErrorResponse("Invalid password"));
            return this;
        }

        // Marcar como online
        AppUser onlineUser = new AppUser(
            user.userId(),
            user.username(),
            user.password(),
            user.contactIds(),
            user.createdAt(),
            true
        );
        usersById.put(userId, onlineUser);
        
        cmd.replyTo.tell(new UserLoggedIn(onlineUser));
        return this;
    }

    private Behavior<Command> onGetUser(GetUser cmd) {
        AppUser user = usersById.get(cmd.userId);
        if (user != null) {
            cmd.replyTo.tell(new UserFound(user));
        } else {
            cmd.replyTo.tell(new ErrorResponse("User not found"));
        }
        return this;
    }

    private Behavior<Command> onGetAllUsers(GetAllUsers cmd) {
        cmd.replyTo.tell(new AllUsers(new ArrayList<>(usersById.values())));
        return this;
    }

    private Behavior<Command> onAddContact(AddContact cmd) {
        AppUser user = usersById.get(cmd.userId);
        AppUser contact = usersById.get(cmd.contactId);
        
        if (user == null || contact == null) {
            cmd.replyTo.tell(new ErrorResponse("User or contact not found"));
            return this;
        }

        if (user.contactIds().contains(cmd.contactId)) {
            cmd.replyTo.tell(new ErrorResponse("Contact already exists"));
            return this;
        }

        List<String> updatedContacts = new ArrayList<>(user.contactIds());
        updatedContacts.add(cmd.contactId);
        
        AppUser updatedUser = new AppUser(
            user.userId(),
            user.username(),
            user.password(),
            updatedContacts,
            user.createdAt(),
            user.online()
        );
        usersById.put(cmd.userId, updatedUser);
        
        cmd.replyTo.tell(new ContactAdded(cmd.userId, cmd.contactId));
        return this;
    }

    private Behavior<Command> onGetContacts(GetContacts cmd) {
        AppUser user = usersById.get(cmd.userId);
        
        if (user == null) {
            cmd.replyTo.tell(new ErrorResponse("User not found"));
            return this;
        }

        List<Contact> contacts = user.contactIds().stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(u -> new Contact(u.userId(), u.username(), 
                    "Hey there! I'm using Beepit", u.online()))
                .collect(Collectors.toList());
        
        cmd.replyTo.tell(new ContactList(contacts));
        return this;
    }

    private Behavior<Command> onSetUserOnline(SetUserOnline cmd) {
        AppUser user = usersById.get(cmd.userId);
        if (user != null) {
            AppUser updatedUser = new AppUser(
                user.userId(),
                user.username(),
                user.password(),
                user.contactIds(),
                user.createdAt(),
                cmd.online
            );
            usersById.put(cmd.userId, updatedUser);
        }
        return this;
    }
}
