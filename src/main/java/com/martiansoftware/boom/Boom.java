package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.permissions;
import com.martiansoftware.boom.auth.FormLoginFilter;
import com.martiansoftware.boom.auth.User;
import com.martiansoftware.dumbtemplates.DumbTemplate;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import org.eclipse.jetty.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ExceptionHandler;
import spark.Filter;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;
import spark.Route;
import spark.Session;
import spark.Spark;
import spark.TemplateEngine;
import spark.TemplateViewRoute;
import spark.route.HttpMethod;
import spark.utils.MimeParse;

/**
 *
 * @author mlamb
 */
public class Boom {

    private Boom() {}
    
    /**
     * If not null, used to authenticate all requests.  
     */
    private static volatile Filter _loginFilter = null;

    /**
     * If not null and nonempty, require these permissions for all subsequent requests
     */
    private static volatile Object[] _permissions = null;
    
    private static final Logger log = LoggerFactory.getLogger(Boom.class);
    private static final boolean _debug;
    
    private static final ThreadLocal<BoomContext> _boomContext = new ThreadLocal<>();
    private static ContextFactory _templateContextFactory = java.util.HashMap::new;

    private static final DumbTemplateStore _templates;
    private static final PathResolver _pathResolver = new PathResolver("/");
    private static final List<Filter> _beforeFilters = new java.util.LinkedList<>();
    private static final List<Filter> _afterFilters = new java.util.LinkedList<>();
    private static final Object _lock = new Object();
    private static boolean _initializedStaticContent = false;
    
    static volatile Path _tmp = Paths.get(System.getProperty("java.io.tmpdir"));
    
    static {
        // TODO: allow port and static content to be done before routes are added?
        initStaticContent();
        initThreadLocalsFilter();
        initLoginFilter();
        
        _debug = Debug.init();
        _templates = Templates.init();
    }
    
    /**
     * Is Boom running in debug mode?  Debug mode allows reloading of changed
     * static files and templates from the source tree rather than loading once
     * from the classpath, and enables the helper application at /boom-debug
     * 
     * @return true if Boom is running in debug mode
     */
    public static boolean debug() { return _debug; }
    
    /**
     * Returns the Spark Request that is currently being serviced
     * @return the Spark Request that is currently being serviced
     */
    public static Request request() { return _boomContext.get().request; }

    /**
     * Returns the Spark Response that is currently being serviced
     * @return the Spark Response that is currently being serviced
    */
    public static Response response() { return _boomContext.get().response; }
    
    public static String resolvePath(String path) { return _pathResolver.resolve(path).toString(); }

    /**
     * Configures a login filter.  A login filter is applied early on in the
     * filter set to require user logins.  See FormAuthFilter.
     * 
     * @param newLoginFilter 
     */
    public static void login(Filter newLoginFilter) {
        _loginFilter = newLoginFilter;
    }

    /**
     * If called while handling a request, verifies that the current User (if any)
     * has ALL of the require permissions; if called during server setup, notes
     * the required permissions and automatically adds a check before all subsequently
     * added routes.
     * 
     * @param perms 
     */
    public static void permissions(Object... perms) {
        if (isRequestThread()) { // processing a request, so check permissions NOW!            
            if (perms == null || perms.length == 0) return; // no perms needed, OK to continue
            log.warn("perms = {}", perms);
            Optional<User> ouser = user();
            if (!ouser.isPresent()) halt(403); // if there's no user they can't possibly have the right permissions
            
            boolean authorized = true;
            User user = ouser.get();
            for (Object p : perms) {
                if (!user.hasPermission(p)) {
                    authorized = false;
                    break;
                }
            }
            if (!authorized) halt(403);
        } else { // configuring server, so note permissions to be checked during boomwrap()
            _permissions = (perms == null || perms.length == 0) ? null : Arrays.copyOf(perms, perms.length);
        }        
    }
    
    public static void locale(Locale locale) {
        session(true).attribute(Constants.LOCALE_ATTRIBUTE, locale);
    }
    
    public static Locale locale() { 
        Locale result =  session(true).attribute(Constants.LOCALE_ATTRIBUTE);
        if (result == null) {
            result = request().raw().getLocale();
            locale(result);
        }
        return result;
    }
    
    /**
     * Set the temp directory for Boom's per-handler temp subdirectories
     * @param tmp the temp directory to use
     * @return the temp directory
     */
    public static Path tmp(Path tmp) { _tmp = tmp; return _tmp; }
    
    /**
     * Obtain a handler-specific temporary directory, creating it if necessary
     * @return a handler-specific temporary directory
     * @throws IOException 
     */
    public static Path tmp() throws IOException { return _boomContext.get().tmp(); }
    
    /**
     * Specify a ContextFactory to provide a preinitialized or otherwise specialized context
     * @param cf the ContextFactory to use for all requests
     */
    public static void contextFactory(ContextFactory cf) { _templateContextFactory = cf; }
    
    /**
     * Convert a path to a canonical form. All instances of "." and ".." are
     * factored out. "/" is returned if the path tries to .. above its root.
     */
    public static String canonicalPath(String path) {
        String result = URIUtil.canonicalPath(path);
        return result == null ? "/" : result;
    }
    
    public static ResourceBundle r(String bundleName) {
        ResourceBundle result = null;
        try { result = ResourceBundle.getBundle("bundles." + bundleName); } catch (MissingResourceException ohWell) {};
        if (result == null) result = ResourceBundle.getBundle("boom-default-bundles." + bundleName);
        return result;
    }
    
    public static Optional<User> user() {
        return FormLoginFilter.currentUser();
    }
    
    /**
     * Returns the specified query param from the current request
     * @param paramName the query param to get
     * @return the specified query param from the current request
     */
    public static Optional<String> q(String paramName) {
        String s = request().queryParams(paramName);
        return s == null ? Optional.empty() : Optional.of(s);
    }
        
    /**
     * Returns the specified header value from the current request
     * @param headerName the header name to get
     * @return the specified header value from the current request
     */
    public static String h(String headerName) {
        return request().headers(headerName);
    }
    
    /**
     * Given one or more encoding options for a response, selects the one most
     * preferred by the current client.  If none of the supported encodings are
     * acceptable by the client or if an unknown MIME type is specified, the
     * request is halted with status 406 (Not Acceptable).
     * @param supportedMimeTypes the MIME types that you as the developer are
     * prepared to return
     * @return the client's preferred selection of the supported MIME types, or
     * else will halt with status 406 (Not Acceptable)
     */
    public static MimeType preferredEncodingOf(MimeType... supportedMimeTypes) {
        Collection<String> supported = Arrays.asList(supportedMimeTypes).stream().map(m -> m.toString()).collect(Collectors.toSet());
        String bestMatch = MimeParse.bestMatch(supported, h("Accept"));
        if (bestMatch.equals(MimeParse.NO_MIME_TYPE)) halt(406);
        MimeType m = MimeType.forName(bestMatch);
        if (m == null) halt(406);
        return m;
    }
    
    /**
     * Returns the working template context
     * @return the working template context
     */
    public static Map<String, Object> context() { return _boomContext.get().templateContext; }
    public static void context(String key, Object value) { context().put(key, value); }
    public static Object context(String key) { return context().get(key); }
    
    // simple redirect shorthand - makes it easier to redirect from a simpleroute
    public static Object redirect(String location, int statusCode) { response().redirect(location, statusCode); return null; }
    public static Object redirect(String location) { response().redirect(location); return null; }
    
    // basic template accessors
    public static DumbTemplate template(String templatePath) { return _templates.get(templatePath); }
    public static DumbTemplateStore templateStore() { return _templates; }

    // TODO: clientSession() for play-framework-like remote sessions
    public static Session session(boolean create) { return request().session(create); }
    public static Session session() { return session(true); }

    // straight passthrough to Spark methods
    public static ModelAndView modelAndView(Object model, String viewName) { return Spark.modelAndView(model, viewName); }
    public static void halt() { Spark.halt(); }
    public static void halt(int status) { Spark.halt(status); } // TODO: lookup standard messages
    public static void halt(int status, String body) { Spark.halt(status, body); }
//        context("status", status);
//        context("body", body); // TODO: default body if null
//        
//    // TODO: look for status-specific page
//        DumbTemplate t = Boom.template("/boom/status/" + status + ".html");
//        if (t == null) t = Boom.template("/boom/status/default.html");
//        Spark.halt(status, t.render(context()));
//    }    
    
    public static void halt(String body) { Spark.halt(body); }
    
    
    public static synchronized void exception(Class<? extends Exception> exceptionClass, ExceptionHandler handler) { Spark.exception(exceptionClass, handler); }
        
    public static void before(Filter filter) { _beforeFilters.add(filter); }
    public static void before(String path, Filter filter) { Spark.before(path, filter); }
    public static void before(String path, String acceptType, Filter filter) { Spark.before(path, acceptType, filter); }
    public static boolean removeBefore(Filter filter) { return _beforeFilters.remove(filter); }
    public static void clearBefore() { _beforeFilters.clear(); }
    
    public static void after(Filter filter) { _afterFilters.add(filter); }
    public static void after(String path, Filter filter) { Spark.after(path, filter); }
    public static void after(String path, String acceptType, Filter filter) { Spark.after(path, acceptType, filter); }
    public static boolean removeAfter(Filter filter) { return _afterFilters.remove(filter); }
    public static void clearAfter() { _afterFilters.clear(); }

    static void postRequestCleanup() {
        if (isRequestThread()) _boomContext.get().cleanup();
    }
    
    static Route boomwrap(Route route) {
        Route wrapped = route;
        if (_permissions != null && _permissions.length > 0) {
            Object[] permsCopy = _permissions; // copy perms at time of adding route
            wrapped = (req,rsp) -> {
                permissions(permsCopy);
                return route.handle(req, rsp);
            };
        }
        return new RouteWrapper(wrapped);
    }
    
    protected static TemplateViewRoute boomwrap(TemplateViewRoute route) {
        TemplateViewRoute wrapped = route;
        if (_permissions != null && _permissions.length > 0) {
            Object[] permsCopy = _permissions; // copy perms at time of adding route
            wrapped = (req,rsp) -> {
                permissions(permsCopy);
                return route.handle(req, rsp);
            };
        }
        return wrapped; // TODO: ensure context cleanup here!
    }    
    
    // below methods set up all the static boom fun stuff
    static void initStaticContent() {
        // static content really should be registered after everything else
        // (since it's mapped to /*) but leaving that up to the user is error-prone,
        // so instead we insert a filter that will run once on the first request
        // to initialize the static content.
        //
        // this static content does NOT honor permissions, but DOES require
        // authentication.
        //
        // TODO: This could be confounded by requests that come in before all
        // of the routes are set up.  This should be fixed.
        log.info("initStaticContent");
        before("/*", (req, rsp) -> {
            synchronized(_lock) {
                if (!_initializedStaticContent) {
                    _permissions = null;
                    get("/*", defaultStaticContentRoute());
                    _initializedStaticContent = true;
                    log.info("Initialized static content!");
                } 
            }
        });
    }

    public static Route defaultStaticContentRoute() {
        ResourceRoute result = new ClasspathResourceRoute(Constants.STATIC_CONTENT_PRODUCTION);
        if (debug()) result = new FilesystemResourceRoute(Constants.STATIC_CONTENT_DEBUG, result);
        return result;        
    }
    
    public static boolean isRequestThread() {
        return _boomContext.get() != null;
    }
    
    private static void initThreadLocalsFilter() {
        Spark.before((Request req, Response rsp) -> {
            log.trace("FILTER: initThreadLocals");
            Map<String, Object> tctx = _templateContextFactory.createContext();
            tctx.put(Constants.BOOM_ROOT, _pathResolver.resolve("/"));
            _boomContext.set(new BoomContext(req, rsp, tctx));            
        });        
    }
    
    private static void initLoginFilter() {
        Spark.before((Request req, Response rsp) -> {
            log.trace("FILTER: loginFilter");
            Filter f = _loginFilter;
            if (f != null) f.handle(req, rsp);
        });
    }
    
    public interface ContextFactory {
        public Map<String, Object> createContext();
    }
    
    private static void addingRoute(HttpMethod method, 
                                    String path, 
                                    String acceptType, 
                                    Route route,
                                    ResponseTransformer transformer,
                                    TemplateViewRoute tvr,
                                    TemplateEngine engine) {
        
        synchronized(_lock) {
            if (_initializedStaticContent) throw new IllegalStateException("Too late to add routes - static content already initialized.");
        }
        
        if (log.isDebugEnabled()) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            log.debug("Adding route for {} {} from {}", method, path, stack[3]);
            log.debug("Adding route with {} filter(s) before and {} after.", _beforeFilters.size(), _afterFilters.size());
        }
        if (acceptType == null) {            
            _beforeFilters.stream().forEach(f -> Spark.before(path, f));
            _afterFilters.stream().forEach(f -> Spark.after(path, f));
        } else {
            _beforeFilters.stream().forEach(f -> Spark.before(path, acceptType, f));
            _afterFilters.stream().forEach(f -> Spark.after(path, acceptType, f));
        }
    }
    
    // a whole bunch of convenient methods for creating BoomResponses of various typs
    public static BoomResponse binary(InputStream in) { return new BoomResponse(in).as(MimeType.BIN); }
    public static BoomResponse binary(File f) throws IOException { return new BoomResponse(f).as(MimeType.BIN).named(f.getName()); }
    public static BoomResponse binary(byte[] b) { return new BoomResponse(new ByteArrayInputStream(b)).as(MimeType.BIN); }
    public static BoomResponse binary(byte[] b, int offset, int len) { return new BoomResponse(new ByteArrayInputStream(b, offset, len)).as(MimeType.BIN); }
    
    public static BoomResponse html(InputStream in) { return new BoomResponse(in).as(MimeType.HTML); }
    public static BoomResponse html(File f) throws IOException { return new BoomResponse(f).as(MimeType.HTML); }
    public static BoomResponse html(String s) { return new BoomResponse(s).as(MimeType.HTML); }
    
    public static BoomResponse json(Object o) { return new BoomResponse(Json.toJson(o)).as(MimeType.JSON); }
    public static BoomResponse json(String s) { return new BoomResponse(s).as(MimeType.JSON); }
    
    public static BoomResponse text(InputStream in) { return new BoomResponse(in).as(MimeType.TXT); }
    public static BoomResponse text(File f) throws IOException { return new BoomResponse(f).as(MimeType.TXT); }
    public static BoomResponse text(String s) { return new BoomResponse(s).as(MimeType.TXT); }
    
    public static BoomResponse xml(InputStream in) { return new BoomResponse(in).as(MimeType.XML); }
    public static BoomResponse xml(File f) throws IOException { return new BoomResponse(f).as(MimeType.XML); }
    public static BoomResponse xml(String s) { return new BoomResponse(s).as(MimeType.XML); }
    
    
    
// the below is created by scripts/updateBoomJava    
// ## BEGIN GENERATED CODE - DO NOT EDIT BELOW THIS LINE ##
    public static synchronized void connect(final String path, final Route route) {
        addingRoute(HttpMethod.connect, path, null, route, null, null, null);
        Spark.connect(path, boomwrap(route));
    }
    
    public static synchronized void connect(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.connect, path, null, route, null, null, null);
        Spark.connect(path, boomwrap(route));
    }
    
    public static synchronized void connect(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.connect, path, null, route, transformer, null, null);
        Spark.connect(path, boomwrap(route), transformer);
    }
    
    public static synchronized void connect(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.connect, path, acceptType, route, null, null, null);
        Spark.connect(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void connect(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.connect, path, acceptType, route, transformer, null, null);
        Spark.connect(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void connect(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.connect, path, acceptType, null, null, route, engine);
        Spark.connect(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void connect(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.connect, path, null, null, null, route, engine);
        Spark.connect(path, boomwrap(route), engine);
    }

    public static synchronized void delete(final String path, final Route route) {
        addingRoute(HttpMethod.delete, path, null, route, null, null, null);
        Spark.delete(path, boomwrap(route));
    }
    
    public static synchronized void delete(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.delete, path, null, route, null, null, null);
        Spark.delete(path, boomwrap(route));
    }
    
    public static synchronized void delete(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.delete, path, null, route, transformer, null, null);
        Spark.delete(path, boomwrap(route), transformer);
    }
    
    public static synchronized void delete(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.delete, path, acceptType, route, null, null, null);
        Spark.delete(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void delete(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.delete, path, acceptType, route, transformer, null, null);
        Spark.delete(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void delete(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.delete, path, acceptType, null, null, route, engine);
        Spark.delete(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void delete(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.delete, path, null, null, null, route, engine);
        Spark.delete(path, boomwrap(route), engine);
    }

    public static synchronized void get(final String path, final Route route) {
        addingRoute(HttpMethod.get, path, null, route, null, null, null);
        Spark.get(path, boomwrap(route));
    }
    
    public static synchronized void get(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.get, path, null, route, null, null, null);
        Spark.get(path, boomwrap(route));
    }
    
    public static synchronized void get(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.get, path, null, route, transformer, null, null);
        Spark.get(path, boomwrap(route), transformer);
    }
    
    public static synchronized void get(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.get, path, acceptType, route, null, null, null);
        Spark.get(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void get(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.get, path, acceptType, route, transformer, null, null);
        Spark.get(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void get(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.get, path, acceptType, null, null, route, engine);
        Spark.get(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void get(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.get, path, null, null, null, route, engine);
        Spark.get(path, boomwrap(route), engine);
    }

    public static synchronized void head(final String path, final Route route) {
        addingRoute(HttpMethod.head, path, null, route, null, null, null);
        Spark.head(path, boomwrap(route));
    }
    
    public static synchronized void head(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.head, path, null, route, null, null, null);
        Spark.head(path, boomwrap(route));
    }
    
    public static synchronized void head(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.head, path, null, route, transformer, null, null);
        Spark.head(path, boomwrap(route), transformer);
    }
    
    public static synchronized void head(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.head, path, acceptType, route, null, null, null);
        Spark.head(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void head(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.head, path, acceptType, route, transformer, null, null);
        Spark.head(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void head(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.head, path, acceptType, null, null, route, engine);
        Spark.head(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void head(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.head, path, null, null, null, route, engine);
        Spark.head(path, boomwrap(route), engine);
    }

    public static synchronized void options(final String path, final Route route) {
        addingRoute(HttpMethod.options, path, null, route, null, null, null);
        Spark.options(path, boomwrap(route));
    }
    
    public static synchronized void options(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.options, path, null, route, null, null, null);
        Spark.options(path, boomwrap(route));
    }
    
    public static synchronized void options(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.options, path, null, route, transformer, null, null);
        Spark.options(path, boomwrap(route), transformer);
    }
    
    public static synchronized void options(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.options, path, acceptType, route, null, null, null);
        Spark.options(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void options(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.options, path, acceptType, route, transformer, null, null);
        Spark.options(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void options(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.options, path, acceptType, null, null, route, engine);
        Spark.options(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void options(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.options, path, null, null, null, route, engine);
        Spark.options(path, boomwrap(route), engine);
    }

    public static synchronized void patch(final String path, final Route route) {
        addingRoute(HttpMethod.patch, path, null, route, null, null, null);
        Spark.patch(path, boomwrap(route));
    }
    
    public static synchronized void patch(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.patch, path, null, route, null, null, null);
        Spark.patch(path, boomwrap(route));
    }
    
    public static synchronized void patch(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.patch, path, null, route, transformer, null, null);
        Spark.patch(path, boomwrap(route), transformer);
    }
    
    public static synchronized void patch(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.patch, path, acceptType, route, null, null, null);
        Spark.patch(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void patch(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.patch, path, acceptType, route, transformer, null, null);
        Spark.patch(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void patch(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.patch, path, acceptType, null, null, route, engine);
        Spark.patch(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void patch(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.patch, path, null, null, null, route, engine);
        Spark.patch(path, boomwrap(route), engine);
    }

    public static synchronized void post(final String path, final Route route) {
        addingRoute(HttpMethod.post, path, null, route, null, null, null);
        Spark.post(path, boomwrap(route));
    }
    
    public static synchronized void post(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.post, path, null, route, null, null, null);
        Spark.post(path, boomwrap(route));
    }
    
    public static synchronized void post(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.post, path, null, route, transformer, null, null);
        Spark.post(path, boomwrap(route), transformer);
    }
    
    public static synchronized void post(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.post, path, acceptType, route, null, null, null);
        Spark.post(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void post(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.post, path, acceptType, route, transformer, null, null);
        Spark.post(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void post(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.post, path, acceptType, null, null, route, engine);
        Spark.post(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void post(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.post, path, null, null, null, route, engine);
        Spark.post(path, boomwrap(route), engine);
    }

    public static synchronized void put(final String path, final Route route) {
        addingRoute(HttpMethod.put, path, null, route, null, null, null);
        Spark.put(path, boomwrap(route));
    }
    
    public static synchronized void put(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.put, path, null, route, null, null, null);
        Spark.put(path, boomwrap(route));
    }
    
    public static synchronized void put(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.put, path, null, route, transformer, null, null);
        Spark.put(path, boomwrap(route), transformer);
    }
    
    public static synchronized void put(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.put, path, acceptType, route, null, null, null);
        Spark.put(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void put(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.put, path, acceptType, route, transformer, null, null);
        Spark.put(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void put(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.put, path, acceptType, null, null, route, engine);
        Spark.put(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void put(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.put, path, null, null, null, route, engine);
        Spark.put(path, boomwrap(route), engine);
    }

    public static synchronized void trace(final String path, final Route route) {
        addingRoute(HttpMethod.trace, path, null, route, null, null, null);
        Spark.trace(path, boomwrap(route));
    }
    
    public static synchronized void trace(final String path, final BoomRoute route) {
        addingRoute(HttpMethod.trace, path, null, route, null, null, null);
        Spark.trace(path, boomwrap(route));
    }
    
    public static synchronized void trace(String path, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.trace, path, null, route, transformer, null, null);
        Spark.trace(path, boomwrap(route), transformer);
    }
    
    public static synchronized void trace(String path, String acceptType, Route route) {
        addingRoute(HttpMethod.trace, path, acceptType, route, null, null, null);
        Spark.trace(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void trace(String path, String acceptType, Route route, ResponseTransformer transformer) {
        addingRoute(HttpMethod.trace, path, acceptType, route, transformer, null, null);
        Spark.trace(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void trace(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.trace, path, acceptType, null, null, route, engine);
        Spark.trace(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void trace(String path, TemplateViewRoute route, TemplateEngine engine) {
        addingRoute(HttpMethod.trace, path, null, null, null, route, engine);
        Spark.trace(path, boomwrap(route), engine);
    }

}
