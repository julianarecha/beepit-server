package com.beepit.server.websocket;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AskPattern;
import com.beepit.server.actor.ActorSystemProvider;
import com.beepit.server.actor.ChatRoomActor;
import com.beepit.server.actor.ConversationManagerActor;
import com.beepit.server.domain.model.Conversation;
import com.beepit.server.domain.model.Message;
import com.beepit.server.domain.model.PrivateMessage;
import com.beepit.server.domain.model.UserSession;
import io.micronaut.http.HttpRequest;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerWebSocket("/ws/chat/{roomId}")
public class ChatWebSocketServerWithAkka {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChatWebSocketServerWithAkka.class);
    
    private final WebSocketBroadcaster broadcaster;
    private final ActorSystemProvider actorSystemProvider;
    private final ObjectMapper objectMapper;
    private final Map<String, UserSessionInfo> sessions = new ConcurrentHashMap<>();
    
    @Inject
    public ChatWebSocketServerWithAkka(
            WebSocketBroadcaster broadcaster,
            ActorSystemProvider actorSystemProvider,
            ObjectMapper objectMapper) {
        this.broadcaster = broadcaster;
        this.actorSystemProvider = actorSystemProvider;
        this.objectMapper = objectMapper;
    }
    
    @OnOpen
    public void onOpen(String roomId, WebSocketSession session, HttpRequest<?> request) {
        String username = request.getParameters()
            .getFirst("username")
            .orElse("Anonymous-" + session.getId().substring(0, 8));
        
        String userId = request.getParameters()
            .getFirst("userId")
            .orElse(username);
            
        LOG.info("WebSocket abierto: room={}, username={}, userId={}, session={}", roomId, username, userId, session.getId());
        
        // Parse room ID to get participant IDs (format: userId1_userId2)
        String[] participants = roomId.split("_");
        String otherUserId = participants[0].equals(userId) ? participants[1] : participants[0];
        
        sessions.put(session.getId(), new UserSessionInfo(session, username, userId, roomId, otherUserId));
        
        // Load conversation history asynchronously
        Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getConversationManagerActor(),
                (ActorRef<ConversationManagerActor.Response> replyTo) -> 
                    new ConversationManagerActor.GetConversation(userId, otherUserId, replyTo),
                Duration.ofSeconds(3),
                actorSystemProvider.getConversationManagerScheduler()
            )
        ).subscribe(response -> {
            LOG.info("GetConversation response for {}->{}: {}", userId, otherUserId, response.getClass().getSimpleName());
            
            if (response instanceof ConversationManagerActor.ConversationFound found) {
                LOG.info("Conversation found with {} messages", found.conversation().messages().size());
                try {
                    // Send conversation history to the client
                    String historyJson = objectMapper.writeValueAsString(Map.of(
                        "type", "history",
                        "messages", found.conversation().messages()
                    ));
                    broadcaster.broadcastSync(historyJson, s -> s.equals(session));
                    LOG.info("History sent to session {}", session.getId());
                } catch (Exception e) {
                    LOG.error("Error serializing history", e);
                }
            } else {
                // No previous conversation
                LOG.info("No conversation found between {} and {}", userId, otherUserId);
                try {
                    String emptyHistory = "{\"type\":\"history\",\"messages\":[]}";
                    broadcaster.broadcastSync(emptyHistory, s -> s.equals(session));
                } catch (Exception e) {
                    LOG.error("Error sending empty history", e);
                }
            }
        }, error -> {
            LOG.error("Error loading conversation", error);
        });
    }
    
    @OnMessage
    public Mono<String> onMessage(String roomId, String messageText, WebSocketSession session) {
        LOG.info("Mensaje recibido: room={}, message={}", roomId, messageText);
        
        UserSessionInfo sessionInfo = sessions.get(session.getId());
        if (sessionInfo == null) {
            return Mono.just("{\"error\":\"Session not found\"}");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(messageText, Map.class);
            String content = (String) data.get("content");
            
            // Send private message through ConversationManagerActor
            LOG.info("Sending message from {} to {}: {}", sessionInfo.userId, sessionInfo.otherUserId, content);
            
            return Mono.fromCompletionStage(
                AskPattern.ask(
                    actorSystemProvider.getConversationManagerActor(),
                    (ActorRef<ConversationManagerActor.Response> replyTo) ->
                        new ConversationManagerActor.SendPrivateMessage(
                            sessionInfo.userId,
                            sessionInfo.otherUserId,
                            content,
                            replyTo
                        ),
                    Duration.ofSeconds(3),
                    actorSystemProvider.getConversationManagerScheduler()
                )
            ).map(response -> {
                LOG.info("SendPrivateMessage response: {}", response.getClass().getSimpleName());
                
                if (response instanceof ConversationManagerActor.MessageSent sent) {
                    LOG.info("Message saved with ID: {}", sent.message().messageId());
                    
                    // Broadcast to both users in the conversation
                    broadcastPrivateMessage(roomId, sent.message(), sessionInfo.userId);
                    
                    try {
                        return objectMapper.writeValueAsString(Map.of(
                            "type", "message_sent",
                            "messageId", sent.message().messageId()
                        ));
                    } catch (Exception e) {
                        return "{}";
                    }
                }
                return "{}";
            });
            
        } catch (Exception e) {
            LOG.error("Error processing message", e);
            return Mono.just("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
    
    @OnClose
    public Mono<Void> onClose(String roomId, WebSocketSession session) {
        LOG.info("WebSocket cerrado: room={}, session={}", roomId, session.getId());
        sessions.remove(session.getId());
        return Mono.empty();
    }
    
    @OnError
    public Mono<Void> onError(String roomId, WebSocketSession session, Throwable error) {
        LOG.error("Error en WebSocket: room={}, session={}", roomId, session.getId(), error);
        return Mono.empty();
    }
    
    private void broadcastPrivateMessage(String roomId, PrivateMessage message, String senderId) {
        try {
            String messageJson = objectMapper.writeValueAsString(Map.of(
                "type", "message",
                "messageId", message.messageId(),
                "sender", senderId,
                "content", message.content(),
                "timestamp", message.timestamp()
            ));
            
            // Broadcast to all sessions in this conversation room
            sessions.values().stream()
                .filter(info -> info.roomId.equals(roomId))
                .forEach(info -> {
                    try {
                        broadcaster.broadcastSync(messageJson, s -> s.equals(info.session));
                    } catch (Exception e) {
                        LOG.error("Error broadcasting to session", e);
                    }
                });
        } catch (Exception e) {
            LOG.error("Error broadcasting message", e);
        }
    }
    
    private record UserSessionInfo(
        WebSocketSession session,
        String username,
        String userId,
        String roomId,
        String otherUserId
    ) {}
}
