package com.beepit.server.actor;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import com.beepit.server.domain.command.UserManagerCommand;
import com.beepit.server.domain.command.UserManagerCommand.*;
import com.beepit.server.domain.response.UserManagerResponse;
import com.beepit.server.domain.response.UserManagerResponse.*;
import com.beepit.server.domain.model.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerActorTest {

    private ActorTestKit testKit;
    private ActorRef<UserManagerCommand> userManager;

    @BeforeEach
    void setup() {
        testKit = ActorTestKit.create();
        userManager = testKit.spawn(UserManagerActor.create());
    }
    
    @AfterEach
    void tearDown() {
        testKit.shutdownTestKit();
    }

    @Test
    void testRegisterUser() {
        TestProbe<UserManagerResponse> probe = testKit.createTestProbe();
        
        userManager.tell(new RegisterUser("testuser", "testpass", probe.getRef()));
        
        UserManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof UserRegistered);
        
        UserRegistered registered = (UserRegistered) response;
        assertEquals("testuser", registered.user().username());
    }

    @Test
    void testLoginUser() {
        TestProbe<UserManagerResponse> probe = testKit.createTestProbe();
        
        // Alice es un usuario pre-cargado
        userManager.tell(new LoginUser("alice", "password123", probe.getRef()));
        
        UserManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof UserLoggedIn);
        
        UserLoggedIn loggedIn = (UserLoggedIn) response;
        assertEquals("alice", loggedIn.user().username().toLowerCase());
    }

    @Test
    void testLoginWithInvalidCredentials() {
        TestProbe<UserManagerResponse> probe = testKit.createTestProbe();
        
        userManager.tell(new LoginUser("alice", "wrongpassword", probe.getRef()));
        
        UserManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof UserManagerResponse.ErrorResponse);
    }

    @Test
    void testAddContact() {
        TestProbe<UserManagerResponse> probe = testKit.createTestProbe();
        
        // Obtener IDs de Alice y Bob
        userManager.tell(new LoginUser("alice", "password123", probe.getRef()));
        UserLoggedIn aliceLogin = (UserLoggedIn) probe.receiveMessage();
        String aliceId = aliceLogin.user().userId();
        
        userManager.tell(new LoginUser("bob", "password123", probe.getRef()));
        UserLoggedIn bobLogin = (UserLoggedIn) probe.receiveMessage();
        String bobId = bobLogin.user().userId();
        
        // Agregar Bob como contacto de Alice
        userManager.tell(new AddContact(aliceId, bobId, probe.getRef()));
        
        UserManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof ContactAdded);
    }

    @Test
    void testGetAllUsers() {
        TestProbe<UserManagerResponse> probe = testKit.createTestProbe();
        
        userManager.tell(new GetAllUsers(probe.getRef()));
        
        UserManagerResponse response = probe.receiveMessage();
        assertTrue(response instanceof AllUsers);
        
        AllUsers allUsers = (AllUsers) response;
        assertTrue(allUsers.users().size() >= 5); // Al menos los 5 usuarios pre-cargados
    }
}
