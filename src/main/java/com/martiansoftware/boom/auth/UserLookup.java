package com.martiansoftware.boom.auth;

import java.util.List;
import java.util.Optional;

/**
 * Provides a means of looking up User objects by their username ("login", etc.).
 * 
 * Implementations MUST accept any form of the username supplied by the user
 * during the login process (mixed upper and lower case, leading and trailing
 * spaces, etc.)
 * 
 * @author mlamb
 */
public interface UserLookup {

    /**
     * Returns the requested User, or Optional.EMPTY if no such user exists.
     * @param username the username (or "login") of the requested user, possibly
     * in a non-canonical form such as mixed upper and lower case and/or with
     * leading/trailing spaces, etc.
     * @return the requested User, or Optional.EMPTY if no such user exists.
     */
    public Optional<User> byName(String username);
    
    /**
     * Returns a List of all Users.
     * @return a List of all Users.
     */
    public List<User> allUsers();
}
