package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.*;

import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Session;

/**
 *
 * @author mlamb
 */
public class FormAuthFilter implements Filter {

//    private static final AppContext app = AppContext.get();
    
    private static final Logger log = LoggerFactory.getLogger(FormAuthFilter.class);
    
    // stores the username in the session.  if this is set the user is considered
    // authenticated.  other stuff can be stored as well, but this is required.
    public static final String USERNAME_KEY = FormAuthFilter.class.getCanonicalName() + ".USERNAME";
    
    // if the user requests a page that requires auth and we redirect them to the
    // login page, this session property stores the originally requested url so
    // we can redirect them after a successful login
    private static final String REDIRECT_KEY = FormAuthFilter.class.getCanonicalName() + ".REDIRECT";
    
    // verifyies username/password
    private final Authenticator authenticator;
    
    // path to login page form (for GET) or where login is submitted (for POST)
    // form must have "u" and "p" fields for username and password, respectively.
    private static final String loginPath = "/login";
    
    // path to logout page.
    private static final String logoutPath = "/logout";
    
    // collection of url paths that do not require authentication.
    private Collection<String> _exempt = new java.util.HashSet<>();
    
    // used to verify username/password.  does not store USERNAME or REDIRECT, but may store
    // other things if necessary
    public interface Authenticator {
        // return the username (in canonical string form) if authenticated, null if not authenticated
        // may put other things in the session
        public String authenticate(String username, String password, spark.Session session);
    }
    
    // creates a new Auth and sets up login/logout paths in spark
    public FormAuthFilter(Authenticator authenticator) {
        this.authenticator = authenticator;
        exempt(loginPath);
        exempt(logoutPath);
        get(loginPath, () -> showForm(null));
        post(loginPath, () -> submitForm());
        get(logoutPath, () -> logout());
    }
        
    /*
     * Exempts a path from authentication requirements.  NOTE: does not (yet?)
     * adhere to spark path semantics - this is just a full path string
     */    
    public FormAuthFilter exempt(String path) { _exempt.add(path); return this;}
    
    @Override
    public void handle(Request rqst, Response rspns) throws Exception {
        log.trace("checking auth requirements for {} to access {}", getUsername(rqst), rqst.pathInfo());
        
        if (_exempt.contains(rqst.pathInfo())) {
            log.trace("exempt: {}", rqst.pathInfo());
            return; // requested page does not require auth; return from filter
                    // and let spark continue normal processing
        }
        
        Session session = session(true);
        if (getUsername(rqst) == null) {
            // not authenticated, so remember where user was trying to go and
            // show them the login page
            log.trace("auth required.");
            String q = rqst.raw().getQueryString();
            StringBuilder u = new StringBuilder(rqst.url());
            if (q != null) {
                u.append('?');
                u.append(q);
            }
            session.attribute(REDIRECT_KEY, u.toString());
            halt(401, showForm(null));
        } else {
            // authenticated, so clear any stored redirection and allow
            // spark to continue normal processing
            log.trace("allowed for {}: {}", getUsername(rqst), rqst.pathInfo());
            session.attribute(REDIRECT_KEY, null);
        }
        
    }
    
    public Object logout() {
        log.info("{} logged out.", getUsername(request()));
        session().invalidate();
        return template("/FormAuthFilter/loggedout.html").render(context()); // TODO: eliminate need for render call
    }
    
    String showForm(String user) {
        try {
            // display the login form with no initial username
            context().put("user", user == null ? "" : user);  // TODO: render nulls as empty strings
            return template("/FormAuthFilter/login.html").render(context());
        } catch (Exception e) {
            // TODO: return better error
            e.printStackTrace();
            return e.getMessage();
        }
    }
    
    // process login attempt
    Object submitForm() {
        String u = request().queryParams("u");
        String p = request().queryParams("p");
        
        String canonicalUsername = authenticator.authenticate(u, p, session());
        if (canonicalUsername != null) {
            // login OK
            log.info("{} logged in.", canonicalUsername);
            session().attribute(USERNAME_KEY, canonicalUsername);
            String url = session().attribute(REDIRECT_KEY);
            if (url == null) url = "/";
            response().redirect(url, 302);
            return null; // not called; redirect throws a HaltException.  needed to satisfy compiler.
        } else {
            context().put("error", r("FormAuthFilter").getString("login_failed"));
            return showForm(u); // badd username or password
        }
    }
    
    // get username (if any) from session (if any)
    public static String getUsername(Request rqst) {
        return rqst.session(true).attribute(USERNAME_KEY);
    }
}
