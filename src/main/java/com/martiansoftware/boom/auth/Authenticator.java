package com.martiansoftware.boom.auth;

/**
 *
 * @author mlamb
 */
public interface Authenticator {    
    public User authenticate(String username, String passphrase);
}
