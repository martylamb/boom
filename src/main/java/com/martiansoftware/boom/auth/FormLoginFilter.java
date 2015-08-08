package com.martiansoftware.boom.auth;

import static com.martiansoftware.boom.Boom.*;

import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Predicate;
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
public class FormLoginFilter implements Filter {

    
    private static final Logger log = LoggerFactory.getLogger(FormLoginFilter.class);
    
    // if the user requests a page that requires auth and we redirect them to the
    // login page, this session property stores the originally requested url so
    // we can redirect them after a successful login
    private static final String REDIRECT_KEY = FormLoginFilter.class.getCanonicalName() + ".REDIRECT";
    
    // verifyies username/password
    private final Authenticator authenticator;
    
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
    public FormLoginFilter(Authenticator authenticator) {
        this.authenticator = authenticator;
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
        
        Session session = session(true);
        User user = session(true).attribute(User.SESSION_KEY);
        if (user == null) {
            // not authenticated, so remember where user was trying to go and
            // show them the login page
            log.trace("authentication required.");
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
            log.trace("authenticated for {}: {}", user.username(), rqst.pathInfo());
            session.attribute(REDIRECT_KEY, null);
        }
        
    }
    
    public Object logout() {
        User user = session(true).attribute(User.SESSION_KEY);
        String uname = (user == null) ? "Anonymous" : user.username();
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

        User user = authenticator.authenticate(u, p);
        if (user != null) {
            // login OK
            log.info("{} logged in.", user.username());
            session(true).attribute(User.SESSION_KEY, user);
            String url = session().attribute(REDIRECT_KEY);
            if (url == null) url = "/";
            response().redirect(url, 302);
            return null; // not called; redirect throws a HaltException.  needed to satisfy compiler.
        } else {
            context().put("error_msg", r("FormAuthFilter").getString("login_failed"));
            return showForm(u); // bad username or password
        }
    }
    
}
