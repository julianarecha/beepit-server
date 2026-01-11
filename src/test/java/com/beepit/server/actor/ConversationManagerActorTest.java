package com.beepit.server.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.beepit.server.domain.command.ConversationManagerCommand;
import com.beepit.server.domain.command.ConversationManagerCommand.*;
import com.beepit.server.domain.response.ConversationManagerResponse;
import com.beepit.server.domain.response.ConversationManagerResponse.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

class ConversationManagerActorTest {

    private ActorTestKit testKit;
    private ActorRef<ConversationManagerCommand> conversationManager;

    @BeforeEach
    void setup() {
        testKit = ActorTestKit.create();
        conversationManager = testKit.spawn(ConversationManagerActor.create());
    }
    
    @AfterEach
    void tearDown() {
        testKit.shutdownTestKit();
    }

    @Test
    void testSendPrivateMessage() {
        TestProbe<ConversationManagerResponse> probe = testKit.createTestProbe();
        
        conversationManager.tell(new SendPrivateMessage(
            "user1", "user2", "Hola!", probe.getRef()
        ));
        
        ConversationManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof MessageSent);
        
        MessageSent sent = (MessageSent) response;
        assertEquals("Hola!", sent.message().content());
        assertEquals("user1", sent.message().senderId());
        assertEquals("user2", sent.message().recipientId());
    }

    @Test
    void testGetConversation() {
        TestProbe<ConversationManagerResponse> probe = testKit.createTestProbe();
        
        // Enviar un mensaje primero
        conversationManager.tell(new SendPrivateMessage(
            "user1", "user2", "Test message", probe.getRef()
        ));
        probe.receiveMessage(); // Consumir respuesta
        
        // Obtener la conversación
        conversationManager.tell(new GetConversation(
            "user1", "user2", probe.getRef()
        ));
        
        ConversationManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof ConversationFound);
        
        ConversationFound found = (ConversationFound) response;
        assertEquals(1, found.conversation().messages().size());
        assertEquals("Test message", found.conversation().messages().get(0).content());
    }

    @Test
    void testGetUserConversations() {
        TestProbe<ConversationManagerResponse> probe = testKit.createTestProbe();
        
        // Crear varias conversaciones para user1
        conversationManager.tell(new SendPrivateMessage(
            "user1", "user2", "Mensaje 1", probe.getRef()
        ));
        probe.receiveMessage();
        
        conversationManager.tell(new SendPrivateMessage(
            "user1", "user3", "Mensaje 2", probe.getRef()
        ));
        probe.receiveMessage();
        
        // Obtener todas las conversaciones de user1
        conversationManager.tell(new GetUserConversations(
            "user1", probe.getRef()
        ));
        
        ConversationManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof ConversationsList);
        
        ConversationsList list = (ConversationsList) response;
        assertEquals(2, list.conversations().size());
    }

    @Test
    void testConversationIdConsistency() {
        TestProbe<ConversationManagerResponse> probe = testKit.createTestProbe();
        
        // Enviar mensaje de user1 a user2
        conversationManager.tell(new SendPrivateMessage(
            "user1", "user2", "Mensaje A", probe.getRef()
        ));
        probe.receiveMessage();
        
        // Enviar mensaje de user2 a user1 (misma conversación)
        conversationManager.tell(new SendPrivateMessage(
            "user2", "user1", "Mensaje B", probe.getRef()
        ));
        probe.receiveMessage();
        
        // Obtener conversación desde user1
        conversationManager.tell(new GetConversation(
            "user1", "user2", probe.getRef()
        ));
        
        ConversationManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof ConversationFound);
        
        ConversationFound found = (ConversationFound) response;
        assertEquals(2, found.conversation().messages().size());
    }
}
