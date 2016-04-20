package com.martiansoftware.boom.auth;

import com.martiansoftware.boom.Boom;
import static com.martiansoftware.boom.Boom.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Session;

/**
 * TODO: document design here.
 * 
 * @author mlamb
 */
public class FormLoginFilter implements Filter, SessionKiller {

    
    private static final Logger log = LoggerFactory.getLogger(FormLoginFilter.class);
    
    // if the user requests a page that requires auth and we redirect them to the
    // login page, this request attribute stores the originally requested url so
    // we can redirect them after a successful login
    private static final String REDIRECT_REQUEST_KEY = FormLoginFilter.class.getCanonicalName() + ".REDIRECT";
    
    // stores the current user in the request scope for use by the application
    private static final String USER_REQUEST_KEY = FormLoginFilter.class.getCanonicalName() + ".USER";
    
    // stores current user info in the session
    private static final String USERINFO_SESSION_KEY = FormLoginFilter.class.getCanonicalName() + ".USERINFO";
    
    // verifyies username/password
    private final UserLookup _userLookup;
    
    // tracks all active sessions by canonical username
    private final Map<String, Set<Session>> _sessionsByCanonicalUsername = new java.util.HashMap<>();
    
    // path to login page form (for GET) or where login is submitted (for POST)
    // form must have "u" and "p" fields for username and password, respectively.
    private static final String loginPath = "/login";
    
    // path to logout page.
    private static final String logoutPath = "/logout";
    
    // collection of url paths that do not require authentication.
    private final Collection<String> _exempt = new java.util.HashSet<>();
    
    // collection of url prefixes that do not require authentication.
    private final Collection<String> _exemptPrefixes = new java.util.HashSet<>();
    
    // collection of predicates that can evaluate a url path to determine if it is exempt from authentication requirements
    private final Collection<Predicate<String>> _exemptPredicates = new java.util.HashSet<>();
    
    // creates a new Auth and sets up login/logout paths in spark
    public FormLoginFilter(UserLookup userLookup) {
        this._userLookup = userLookup;
        exempt(loginPath);
        exempt(logoutPath);
        get(loginPath, () -> showForm(null));
        post(loginPath, () -> submitForm());
        get(logoutPath, () -> logout());
    }
        
    /*
     * Exempts a path from authentication requirements.  NOTE: does not (yet?)
     * adhere to spark path semantics - this is just a simple path string
     * (e.g. /a/b/c.html)
     */    
    public FormLoginFilter exempt(String path) { _exempt.add(path); return this;}
    
    /**
     * Exempts a path prefix from authentication requirements.  For example,
     * to exempt all URLs under the css directory, you can exemptPrefix("/css/");
     * It's probably a good idea to include the trailing slash; otherwise,
     * the above would also exempt "/css-private" because it starts with "/css".
     * 
     * @param prefix the path prefix to exempt from authentication requirements
     * @return this FormLoginFilter
     */   
    public FormLoginFilter exemptPrefix(String prefix) { _exemptPrefixes.add(prefix); return this;}
    
    /**
     * Exempts paths that satisfy the Predicate from authentication requirements.
     * 
     * @param pred the predicate to evaluate against URL paths (e.g., "/index.html")
     * @return this FormLoginFilter
     */
    public FormLoginFilter exempt(Predicate<String> pred) { _exemptPredicates.add(pred); return this;}
    
    /**
     * Returns true if the specified path is exempt from authentication requirements
     * @param pathInfo the path to check (e.g., "/index.html")
     * @return true if the specified path is exempt from authentication requirements
     */
    public boolean isExempt(String pathInfo) {
        if (_exempt.contains(pathInfo)) return true;
        if (_exemptPrefixes.stream().anyMatch(prefix -> pathInfo.startsWith(prefix))) return true;
        return (_exemptPredicates.stream().anyMatch(pred -> pred.test(pathInfo)));
    }
    
    @Override
    public void handle(Request rqst, Response rspns) throws Exception {
        log.trace("checking authentication requirements to access {}", rqst.pathInfo());
        
        if (isExempt(rqst.pathInfo())) {
            log.trace("exempt: {}", rqst.pathInfo());
            return; // requested page does not require auth; return from filter
                    // and let spark continue normal processing
        }
        
        UserInfo userInfo = session(true).attribute(USERINFO_SESSION_KEY);
        Optional<? extends User> oUser = (userInfo == null) ? Optional.empty() : _userLookup.byName(userInfo.canonicalName());
        if (!oUser.isPresent()) {
            // not authenticated, so remember where user was trying to go and
            // show them the login page
            log.warn("authentication required.");
            String q = rqst.queryString();
            StringBuilder u = new StringBuilder(rqst.url());
            if (q != null && q.length() > 0) {
                u.append('?');
                u.append(q);
            }
            rqst.attribute(REDIRECT_REQUEST_KEY, u.toString());
            halt(401, showForm(null));
        } else {
            // authenticated, so clear any stored redirection, store the user in
            // the request so that the app can access it, allow spark to continue normal processing
            log.trace("authenticated for {}: {}", oUser.get().canonicalName(), rqst.pathInfo());
            rqst.attribute(USER_REQUEST_KEY, oUser.get());
            rqst.attribute(REDIRECT_REQUEST_KEY, null);
        }
        
    }
    
    public static Optional<User> currentUser() {
        if (Boom.isRequestThread()) {
            User user = request().attribute(USER_REQUEST_KEY);
            if (user != null) return Optional.of(user);
        }
        return Optional.empty();
    }
    
    public Object logout() {
        UserInfo uinfo = session(true).attribute(USERINFO_SESSION_KEY);
        String uname = (uinfo == null) ? "Anonymous" : uinfo.canonicalName();
        log.info("{} logged out.", uname);
        session().invalidate();
        return template("/FormAuthFilter/loggedout.html");
    }
    
    // utility method to copy specific resource strings from a resource bundle into the current template context
    // TODO: pull this out as a boom utility function?
    private void cResources(String bundle, String... resources) {
        ResourceBundle r = r(bundle);
        for (String resource : resources) context(resource, r.getString(resource));
    }
    
    String showForm(String user) {
        try {
            // display the login form with no initial username
            context("user", user == null ? "" : user);  // TODO: render nulls as empty strings
            context("loginPath", loginPath);
            context("title", r("FormAuthFilter").getString("login_title"));
            context("url", request().attribute(REDIRECT_REQUEST_KEY));
            cResources("FormAuthFilter", "login_user_prompt", "login_passphrase_prompt", "login_button_label");
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
        String url = request().queryParams("url");
        
        User user = _userLookup.byName(u).orElse(null);
        if (user != null && user.authenticate(p.toCharArray())) {
            // login OK
            log.info("{} logged in.", user.canonicalName());
            session(true).attribute(USERINFO_SESSION_KEY, new UserInfo(user.canonicalName()));
            if (url == null) url = "/"; // TODO: compute a better default URL (parent of "/login" ?)
            response().redirect(url, 302);
            return null; // not called; redirect throws a HaltException.  needed to satisfy compiler.
        } else {
            request().attribute(REDIRECT_REQUEST_KEY, url);
            context().put("error_msg", r("FormAuthFilter").getString("login_failed"));
            return showForm(u); // bad username or password
        }
    }
    
    void registerSession(String canonicalUsername) {
        synchronized(_sessionsByCanonicalUsername) {
            Set<Session> sessions = _sessionsByCanonicalUsername.get(canonicalUsername);
            if (sessions == null) {
                sessions = new java.util.HashSet<>();
                _sessionsByCanonicalUsername.put(canonicalUsername, sessions);
            }
            sessions.add(session());
            log.info("Registered new session for user [{}]", canonicalUsername);
        }
    }
    
    void unregisterSession(String canonicalUsername) {
        synchronized(_sessionsByCanonicalUsername) {
            Set<Session> sessions = _sessionsByCanonicalUsername.get(canonicalUsername);
            if (sessions == null) return; // should never happen... TODO warn because wtf?
            sessions.remove(session());
            if (sessions.isEmpty()) _sessionsByCanonicalUsername.remove(canonicalUsername);
            log.info("Unregistered session for user [{}]", canonicalUsername);
        }
    }
    
    public void killSessionsFor(String canonicalUsername) {
        synchronized(_sessionsByCanonicalUsername) {
            Set<Session> sessions = _sessionsByCanonicalUsername.remove(canonicalUsername);
            if (sessions == null) return; // nothing to do
            sessions.stream().map(s -> s.raw()).forEach(s -> { if (s != null) s.invalidate(); });
        }
    }
    
    public void killSessionsFor(User user) { killSessionsFor(user.canonicalName()); }
    
    private class UserInfo implements HttpSessionBindingListener {
        private final String _canonicalUsername;
        public UserInfo(String canonicalUsername) { _canonicalUsername = canonicalUsername; }
        public String canonicalName() { return _canonicalUsername; }
        @Override public void valueBound(HttpSessionBindingEvent hsbe) {
            registerSession(_canonicalUsername);
        }
        @Override public void valueUnbound(HttpSessionBindingEvent hsbe) {
            unregisterSession(_canonicalUsername);
        }
    }
}
