package com.beepit.server.domain.response;

import com.beepit.server.domain.model.AppUser;
import com.beepit.server.domain.model.Contact;

import java.util.List;

public sealed interface UserManagerResponse 
    permits UserManagerResponse.UserRegistered,
            UserManagerResponse.UserLoggedIn,
            UserManagerResponse.UserFound,
            UserManagerResponse.AllUsers,
            UserManagerResponse.ContactAdded,
            UserManagerResponse.ContactList,
            UserManagerResponse.ErrorResponse {
    
    public record UserRegistered(AppUser user) implements UserManagerResponse {}
    public record UserLoggedIn(AppUser user) implements UserManagerResponse {}
    public record UserFound(AppUser user) implements UserManagerResponse {}
    public record AllUsers(List<AppUser> users) implements UserManagerResponse {}
    public record ContactAdded(String userId, String contactId) implements UserManagerResponse {}
    public record ContactList(List<Contact> contacts) implements UserManagerResponse {}
    public record ErrorResponse(String message) implements UserManagerResponse {}
}
