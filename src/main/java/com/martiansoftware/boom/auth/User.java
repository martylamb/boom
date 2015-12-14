package com.martiansoftware.boom.auth;

import com.martiansoftware.boom.Boom;
import java.util.Optional;
import java.util.Set;
import spark.Session;
import spark.Spark;

/**
 *
 * @author mlamb
 */
public interface User {

    /**
     * Returns this User's canonical username (trimmed, perhaps all lowercase,
     * whatever is appropriate for the application).  All non-canonical forms
     * of the same username MUST resolve to the same canonical username.
     * 
     * @return this User's canonical username
     */
    public String canonicalName();
    
    /**
     * Attempts to authenticate this User with the supplied passphrase
     * @param passphrase
     * @return true if the authentication is successful, false otherwise.
     */
    public boolean authenticate(char[] passphrase);
    
    /**
     * Indicates whether this User has the specified permission.  Any Object
     * at all may be used as a permission; in general it's a good idea to
     * use an Enum for this.
     * @param permission the permission being checked for
     * @return true if this User has the specified permission, false otherwise.
     */
    public boolean hasPermission(Object permission);
    
//    /**
//     * Returns the current logged-in user, if any
//     * @return the current logged-in user, if any
//     */
//    public static Optional<User> current() {
//        User user = null;
//        if (Boom.isRequestThread()) {
//            Session session = Boom.session(false);
//            if (session != null) {
//                user = session.attribute(SESSION_KEY);
//            }
//        }
//        return user == null ? Optional.empty() : Optional.of(user);        
//    }
}
