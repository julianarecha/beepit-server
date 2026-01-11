package com.beepit.server.controller;

import akka.actor.typed.javadsl.AskPattern;
import com.beepit.server.actor.ActorSystemProvider;
import com.beepit.server.actor.ConversationManagerActor;
import com.beepit.server.actor.UserManagerActor;
import com.beepit.server.domain.model.AppUser;
import com.beepit.server.domain.model.Contact;
import com.beepit.server.domain.model.Conversation;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Controller("/api/auth")
public class AuthController {

    private final ActorSystemProvider actorSystemProvider;

    @Inject
    public AuthController(ActorSystemProvider actorSystemProvider) {
        this.actorSystemProvider = actorSystemProvider;
    }

    @Post("/register")
    public Mono<HttpResponse<?>> register(@Body RegisterRequest request) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerActor.Response> replyTo) -> 
                    new UserManagerActor.RegisterUser(request.username, request.password, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getUserManagerScheduler()
            )
        ).map(response -> {
            if (response instanceof UserManagerActor.UserRegistered registered) {
                return HttpResponse.ok(new AuthResponse(
                    registered.user().userId(),
                    registered.user().username(),
                    "User registered successfully"
                ));
            } else if (response instanceof UserManagerActor.ErrorResponse error) {
                return HttpResponse.badRequest(new ErrorDTO(error.message()));
            }
            return HttpResponse.serverError(new ErrorDTO("Unknown error"));
        });
    }

    @Post("/login")
    public Mono<HttpResponse<?>> login(@Body LoginRequest request) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerActor.Response> replyTo) -> 
                    new UserManagerActor.LoginUser(request.username, request.password, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getUserManagerScheduler()
            )
        ).map(response -> {
            if (response instanceof UserManagerActor.UserLoggedIn loggedIn) {
                return HttpResponse.ok(new AuthResponse(
                    loggedIn.user().userId(),
                    loggedIn.user().username(),
                    "Login successful"
                ));
            } else if (response instanceof UserManagerActor.ErrorResponse error) {
                return HttpResponse.unauthorized().body(new ErrorDTO(error.message()));
            }
            return HttpResponse.serverError(new ErrorDTO("Unknown error"));
        });
    }

    @Get("/contacts/{userId}")
    public Mono<HttpResponse<?>> getContacts(@PathVariable String userId) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerActor.Response> replyTo) -> 
                    new UserManagerActor.GetContacts(userId, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getUserManagerScheduler()
            )
        ).map(response -> {
            if (response instanceof UserManagerActor.ContactList contactList) {
                return HttpResponse.ok(contactList.contacts());
            } else if (response instanceof UserManagerActor.ErrorResponse error) {
                return HttpResponse.notFound(new ErrorDTO(error.message()));
            }
            return HttpResponse.serverError(new ErrorDTO("Unknown error"));
        });
    }

    @Get("/users")
    public Mono<HttpResponse<List<AppUser>>> getAllUsers() {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerActor.Response> replyTo) -> 
                    new UserManagerActor.GetAllUsers(replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getUserManagerScheduler()
            )
        ).map(response -> {
            if (response instanceof UserManagerActor.AllUsers allUsers) {
                return HttpResponse.ok(allUsers.users());
            }
            return HttpResponse.serverError();
        });
    }

    @Post("/contacts/add")
    public Mono<HttpResponse<?>> addContact(@Body AddContactRequest request) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerActor.Response> replyTo) -> 
                    new UserManagerActor.AddContact(request.userId, request.contactId, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getUserManagerScheduler()
            )
        ).map(response -> {
            if (response instanceof UserManagerActor.ContactAdded) {
                return HttpResponse.ok(new MessageDTO("Contact added successfully"));
            } else if (response instanceof UserManagerActor.ErrorResponse error) {
                return HttpResponse.badRequest(new ErrorDTO(error.message()));
            }
            return HttpResponse.serverError(new ErrorDTO("Unknown error"));
        });
    }
    
    @Get("/conversations/{userId}")
    public Mono<HttpResponse<?>> getUserConversations(@PathVariable String userId) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getConversationManagerActor(),
                (akka.actor.typed.ActorRef<ConversationManagerActor.Response> replyTo) -> 
                    new ConversationManagerActor.GetUserConversations(userId, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getConversationManagerScheduler()
            )
        ).flatMap(response -> {
            if (response instanceof ConversationManagerActor.ConversationsList found) {
                // Get user's contacts to identify non-contacts
                return Mono.fromCompletionStage(
                    AskPattern.ask(
                        actorSystemProvider.getUserManagerActor(),
                        (akka.actor.typed.ActorRef<UserManagerActor.Response> replyTo) -> 
                            new UserManagerActor.GetContacts(userId, replyTo),
                        Duration.ofSeconds(3),
                        actorSystemProvider.getUserManagerScheduler()
                    )
                ).map(contactsResponse -> {
                    if (contactsResponse instanceof UserManagerActor.ContactList contactsFound) {
                        List<String> contactIds = contactsFound.contacts().stream()
                            .map(Contact::userId)
                            .collect(Collectors.toList());
                        
                        // Separate conversations into contacts and non-contacts
                        List<ConversationInfo> conversations = found.conversations().stream()
                            .map(conv -> {
                                String otherUserId = conv.participants().stream()
                                    .filter(id -> !id.equals(userId))
                                    .findFirst()
                                    .orElse("unknown");
                                boolean isContact = contactIds.contains(otherUserId);
                                int unreadCount = (int) conv.messages().stream()
                                    .filter(msg -> !msg.senderId().equals(userId) && !msg.read())
                                    .count();
                                return new ConversationInfo(
                                    conv.conversationId(),
                                    otherUserId,
                                    isContact,
                                    unreadCount,
                                    conv.messages().isEmpty() ? null : 
                                        conv.messages().get(conv.messages().size() - 1).content()
                                );
                            })
                            .collect(Collectors.toList());
                        
                        return HttpResponse.ok(conversations);
                    }
                    return HttpResponse.ok(List.of());
                });
            }
            return Mono.just(HttpResponse.ok(List.of()));
        });
    }

    @Serdeable
    public record RegisterRequest(String username, String password) {}

    @Serdeable
    public record LoginRequest(String username, String password) {}

    @Serdeable
    public record AddContactRequest(String userId, String contactId) {}

    @Serdeable
    public record AuthResponse(String userId, String username, String message) {}

    @Serdeable
    public record ErrorDTO(String error) {}

    @Serdeable
    public record MessageDTO(String message) {}
    
    @Serdeable
    public record ConversationInfo(
        String conversationId,
        String otherUserId,
        boolean isContact,
        int unreadCount,
        String lastMessage
    ) {}
}
