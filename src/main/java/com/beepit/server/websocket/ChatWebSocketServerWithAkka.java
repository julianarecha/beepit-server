package com.beepit.server.websocket;

import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AskPattern;
import com.beepit.server.actor.ActorSystemProvider;
import com.beepit.server.domain.command.ConversationManagerCommand.*;
import com.beepit.server.domain.response.ConversationManagerResponse;
import com.beepit.server.domain.response.ConversationManagerResponse.*;
import com.beepit.server.domain.model.PrivateMessage;
import com.beepit.server.service.RateLimiterService;
import io.micronaut.http.HttpRequest;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ServerWebSocket("/ws/chat/{roomId}")
public class ChatWebSocketServerWithAkka {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChatWebSocketServerWithAkka.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration FAST_TIMEOUT = Duration.ofSeconds(2);
    
    private final WebSocketBroadcaster broadcaster;
    private final ActorSystemProvider actorSystemProvider;
    private final ObjectMapper objectMapper;
    private final RateLimiterService rateLimiterService;
    
    // Mapa optimizado: userId -> lista de sesiones
    private final Map<String, List<UserSessionInfo>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, UserSessionInfo> sessionById = new ConcurrentHashMap<>();
    
    @Inject
    public ChatWebSocketServerWithAkka(
            WebSocketBroadcaster broadcaster,
            ActorSystemProvider actorSystemProvider,
            ObjectMapper objectMapper,
            RateLimiterService rateLimiterService) {
        this.broadcaster = broadcaster;
        this.actorSystemProvider = actorSystemProvider;
        this.objectMapper = objectMapper;
        this.rateLimiterService = rateLimiterService;
    }
    
    @OnOpen
    public void onOpen(String roomId, WebSocketSession session, HttpRequest<?> request) {
        String userId = extractUserId(request, session);
        String username = extractUsername(request, session);
        String otherUserId = parseOtherUserId(roomId, userId);
        
        MDC.put("userId", userId);
        MDC.put("sessionId", session.getId());
        
        LOG.info("WebSocket abierto: room={}, username={}, userId={}", roomId, username, userId);
        
        registerSession(session, username, userId, roomId, otherUserId);
        loadAndSendHistory(session, userId, otherUserId);
        
        MDC.clear();
    }
    
    @OnMessage
    public Mono<String> onMessage(String roomId, String messageText, WebSocketSession session) {
        UserSessionInfo sessionInfo = sessionById.get(session.getId());
        if (sessionInfo == null) {
            LOG.warn("Mensaje de sesión no registrada: {}", session.getId());
            return Mono.just(errorResponse("Session not found"));
        }
        
        MDC.put("userId", sessionInfo.userId);
        MDC.put("sessionId", session.getId());
        
        // Rate limiting
        if (!rateLimiterService.tryAcquire(sessionInfo.userId)) {
            LOG.warn("Rate limit excedido para usuario: {}", sessionInfo.userId);
            MDC.clear();
            return Mono.just(errorResponse("Rate limit exceeded"));
        }
        
        try {
            String content = extractMessageContent(messageText);
            LOG.debug("Procesando mensaje de {} a {}: {}", sessionInfo.userId, sessionInfo.otherUserId, content);
            
            return sendPrivateMessage(sessionInfo, content, roomId);
        } catch (Exception e) {
            LOG.error("Error procesando mensaje", e);
            return Mono.just(errorResponse(e.getMessage()));
        } finally {
            MDC.clear();
        }
    }
    
    @OnClose
    public void onClose(String roomId, WebSocketSession session) {
        UserSessionInfo sessionInfo = sessionById.remove(session.getId());
        if (sessionInfo != null) {
            unregisterSession(sessionInfo);
            LOG.info("WebSocket cerrado: room={}, userId={}", roomId, sessionInfo.userId);
        }
    }
    
    @OnError
    public void onError(String roomId, WebSocketSession session, Throwable error) {
        UserSessionInfo sessionInfo = sessionById.get(session.getId());
        if (sessionInfo != null) {
            MDC.put("userId", sessionInfo.userId);
            MDC.put("sessionId", session.getId());
        }
        LOG.error("Error en WebSocket: room={}", roomId, error);
        MDC.clear();
    }
    
    // Métodos privados para separar responsabilidades
    
    private String extractUserId(HttpRequest<?> request, WebSocketSession session) {
        return request.getParameters()
            .getFirst("userId")
            .orElse("anonymous-" + session.getId().substring(0, 8));
    }
    
    private String extractUsername(HttpRequest<?> request, WebSocketSession session) {
        return request.getParameters()
            .getFirst("username")
            .orElse("Anonymous-" + session.getId().substring(0, 8));
    }
    
    private String parseOtherUserId(String roomId, String userId) {
        String[] participants = roomId.split("_");
        return participants[0].equals(userId) ? participants[1] : participants[0];
    }
    
    private void registerSession(WebSocketSession session, String username, 
                                 String userId, String roomId, String otherUserId) {
        UserSessionInfo info = new UserSessionInfo(session, username, userId, roomId, otherUserId);
        sessionById.put(session.getId(), info);
        
        // Agregar a la lista de sesiones del usuario para broadcast optimizado
        userSessions.compute(userId, (key, sessions) -> {
            if (sessions == null) {
                sessions = new java.util.concurrent.CopyOnWriteArrayList<>();
            }
            sessions.add(info);
            return sessions;
        });
        
        userSessions.compute(otherUserId, (key, sessions) -> {
            if (sessions == null) {
                sessions = new java.util.concurrent.CopyOnWriteArrayList<>();
            }
            sessions.add(info);
            return sessions;
        });
    }
    
    private void unregisterSession(UserSessionInfo info) {
        userSessions.computeIfPresent(info.userId, (key, sessions) -> {
            sessions.remove(info);
            return sessions.isEmpty() ? null : sessions;
        });
        
        userSessions.computeIfPresent(info.otherUserId, (key, sessions) -> {
            sessions.remove(info);
            return sessions.isEmpty() ? null : sessions;
        });
        
        rateLimiterService.cleanup(info.userId);
    }
    
    private void loadAndSendHistory(WebSocketSession session, String userId, String otherUserId) {
        Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getConversationManagerActor(),
                (ActorRef<ConversationManagerResponse> replyTo) -> 
                    new GetConversation(userId, otherUserId, replyTo),
                FAST_TIMEOUT,
                actorSystemProvider.getScheduler()
            )
        ).subscribe(
            response -> handleHistoryResponse(response, session, userId, otherUserId),
            error -> LOG.error("Error cargando historial para {}->{}", userId, otherUserId, error)
        );
    }
    
    private void handleHistoryResponse(ConversationManagerResponse response, 
                                      WebSocketSession session, String userId, String otherUserId) {
        try {
            String historyJson;
            if (response instanceof ConversationFound found) {
                LOG.debug("Historial encontrado: {} mensajes", found.conversation().messages().size());
                historyJson = objectMapper.writeValueAsString(Map.of(
                    "type", "history",
                    "messages", found.conversation().messages()
                ));
            } else {
                LOG.debug("Sin historial previo entre {} y {}", userId, otherUserId);
                historyJson = "{\"type\":\"history\",\"messages\":[]}";
            }
            broadcaster.broadcastSync(historyJson, s -> s.equals(session));
        } catch (Exception e) {
            LOG.error("Error enviando historial", e);
        }
    }
    
    private String extractMessageContent(String messageText) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.readValue(messageText, Map.class);
        String content = (String) data.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        if (content.length() > 5000) {
            throw new IllegalArgumentException("Message too long (max 5000 characters)");
        }
        return content;
    }
    
    private Mono<String> sendPrivateMessage(UserSessionInfo sessionInfo, String content, String roomId) {
        return Mono.fromCompletionStage(
            AskPattern.ask(
                actorSystemProvider.getConversationManagerActor(),
                (ActorRef<ConversationManagerResponse> replyTo) ->
                    new SendPrivateMessage(
                        sessionInfo.userId,
                        sessionInfo.otherUserId,
                        content,
                        replyTo
                    ),
                DEFAULT_TIMEOUT,
                actorSystemProvider.getScheduler()
            )
        ).map(response -> {
            if (response instanceof MessageSent sent) {
                LOG.debug("Mensaje guardado: {}", sent.message().messageId());
                broadcastToConversation(roomId, sent.message());
                return successResponse(sent.message().messageId());
            }
            return errorResponse("Failed to send message");
        });
    }
    
    private void broadcastToConversation(String roomId, PrivateMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(Map.of(
                "type", "message",
                "messageId", message.messageId(),
                "senderId", message.senderId(),
                "recipientId", message.recipientId(),
                "content", message.content(),
                "timestamp", message.timestamp()
            ));
            
            // Broadcast optimizado: solo a sesiones de usuarios en esta conversación
            List<WebSocketSession> targetSessions = sessionById.values().stream()
                .filter(info -> info.roomId.equals(roomId))
                .map(info -> info.session)
                .collect(Collectors.toList());
            
            for (WebSocketSession session : targetSessions) {
                broadcaster.broadcastSync(messageJson, s -> s.equals(session));
            }
        } catch (Exception e) {
            LOG.error("Error broadcasting mensaje", e);
        }
    }
    
    private String successResponse(String messageId) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "type", "message_sent",
                "messageId", messageId
            ));
        } catch (Exception e) {
            return "{\"type\":\"message_sent\"}";
        }
    }
    
    private String errorResponse(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message));
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
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
