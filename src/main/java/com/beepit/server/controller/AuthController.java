package com.beepit.server.controller;

import akka.actor.typed.javadsl.AskPattern;
import com.beepit.server.actor.ActorSystemProvider;
import com.beepit.server.domain.command.UserManagerCommand.*;
import com.beepit.server.domain.command.ConversationManagerCommand.*;
import com.beepit.server.domain.response.UserManagerResponse;
import com.beepit.server.domain.response.UserManagerResponse.*;
import com.beepit.server.domain.response.ConversationManagerResponse;
import com.beepit.server.domain.response.ConversationManagerResponse.*;
import com.beepit.server.domain.model.AppUser;
import com.beepit.server.domain.model.Contact;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public Mono<HttpResponse<?>> register(@Valid @Body RegisterRequest request) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerResponse> replyTo) -> 
                    new RegisterUser(request.username, request.password, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getScheduler()
            )
        ).map(response -> {
            if (response instanceof UserRegistered registered) {
                // Devolver el objeto AppUser completo
                return HttpResponse.ok(registered.user());
            } else if (response instanceof UserManagerResponse.ErrorResponse error) {
                return HttpResponse.badRequest(new ErrorDTO(error.message()));
            }
            return HttpResponse.serverError(new ErrorDTO("Unknown error"));
        });
    }

    @Post("/login")
    public Mono<HttpResponse<?>> login(@Valid @Body LoginRequest request) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerResponse> replyTo) -> 
                    new LoginUser(request.username, request.password, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getScheduler()
            )
        ).map(response -> {
            if (response instanceof UserLoggedIn loggedIn) {
                // Devolver el objeto AppUser completo que espera el frontend
                return HttpResponse.ok(loggedIn.user());
            } else if (response instanceof UserManagerResponse.ErrorResponse error) {
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
                (akka.actor.typed.ActorRef<UserManagerResponse> replyTo) -> 
                    new GetContacts(userId, replyTo),
                Duration.ofSeconds(2),
                actorSystemProvider.getScheduler()
            )
        ).map(response -> {
            if (response instanceof ContactList contactList) {
                return HttpResponse.ok(contactList.contacts());
            } else if (response instanceof UserManagerResponse.ErrorResponse error) {
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
                (akka.actor.typed.ActorRef<UserManagerResponse> replyTo) -> 
                    new GetAllUsers(replyTo),
                Duration.ofSeconds(2),
                actorSystemProvider.getScheduler()
            )
        ).map(response -> {
            if (response instanceof AllUsers allUsers) {
                return HttpResponse.ok(allUsers.users());
            }
            return HttpResponse.serverError();
        });
    }

    @Post("/contacts/add")
    public Mono<HttpResponse<?>> addContact(@Valid @Body AddContactRequest request) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getUserManagerActor(),
                (akka.actor.typed.ActorRef<UserManagerResponse> replyTo) -> 
                    new AddContact(request.userId, request.contactId, replyTo),
                Duration.ofSeconds(2),
                actorSystemProvider.getScheduler()
            )
        ).map(response -> {
            if (response instanceof ContactAdded) {
                return HttpResponse.ok(new MessageDTO("Contact added successfully"));
            } else if (response instanceof UserManagerResponse.ErrorResponse error) {
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
                (akka.actor.typed.ActorRef<ConversationManagerResponse> replyTo) -> 
                    new GetUserConversations(userId, replyTo),
                Duration.ofSeconds(5),
                actorSystemProvider.getScheduler()
            )
        ).flatMap(response -> {
            if (response instanceof ConversationsList found) {
                // Get user's contacts to identify non-contacts
                return Mono.fromCompletionStage(
                    AskPattern.ask(
                        actorSystemProvider.getUserManagerActor(),
                        (akka.actor.typed.ActorRef<UserManagerResponse> replyTo) -> 
                            new GetContacts(userId, replyTo),
                        Duration.ofSeconds(2),
                        actorSystemProvider.getScheduler()
                    )
                ).map(contactsResponse -> {
                    if (contactsResponse instanceof ContactList contactsFound) {
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
    public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
    ) {}

    @Serdeable
    public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,
        @NotBlank(message = "Password is required")
        String password
    ) {}

    @Serdeable
    public record AddContactRequest(
        @NotBlank(message = "User ID is required")
        String userId,
        @NotBlank(message = "Contact ID is required")
        String contactId
    ) {}

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
