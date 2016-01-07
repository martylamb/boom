package com.martiansoftware.boom.auth;

/**
 *
 * @author mlamb
 */
public interface SessionKiller {

    public void killSessionsFor(String canonicalUsername);
    public default void killSessionsFor(User u) { killSessionsFor(u.canonicalName()); }
    
}
