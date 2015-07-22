package com.martiansoftware.boom.auth;

import com.martiansoftware.boom.Boom;
import java.util.Optional;
import java.util.Set;
import spark.Session;

/**
 *
 * @author mlamb
 */
public class User {

    static final String SESSION_KEY = "BOOM_USER";

    protected final String _username;
    private final Set _permissions = new java.util.HashSet();
    
    /**
     * Creates a new User with the specified username.  The username should
     * be canonical, i.e. in your system's preferred format.  Depending upon
     * your needs, this might mean e.g. converting an email address to lowercase
     * and trimming leading/trailing whitespace.
     * 
     * @param username the CANONICAL username for this user.
     */
    public User(String username) {
        _username = username;
    }
    
    /**
     * Returns the CANONICAL username for this user.  Depending upon your needs,
     * this might mean e.g. converting an email address to lowercase and trimming
     * leading/trailing whitespace.
     * 
     * @return the CANONICAL username for this user.
     */
    public String username() {
        return _username;
    }
    
    /**
     * Indicates whether this user has the specified permission.  Permissions
     * may be any object that provides hashCode() and equals(), which are used
     * in making this determination.
     * 
     * @param perm the permission we're checking for
     * @return true iff this user has the specified permission
     */
    public final boolean hasPermission(Object perm) {
        return _permissions.contains(perm);
    }
    
    /**
     * Grants this user the specified permission.  Permissions may be any object
     * that provides hashCode() and equals(), which are used in determining
     * whether a user has a given permission.
     * 
     * @param perm the permission to add
     * @return this user
     */
    protected final User addPermission(Object perm) {
        _permissions.add(perm);
        return this;
    }

    @Override public String toString() {
        return username();
    }
    
    /**
     * Returns the current logged-in user, if any
     * @return the current logged-in user, if any
     */
    public static Optional<User> current() {
        User user = null;
        if (Boom.isRequestThread()) {
            Session session = Boom.session(false);
            if (session != null) {
                user = session.attribute(SESSION_KEY);
            }
        }
        return user == null ? Optional.empty() : Optional.of(user);
    }
}
