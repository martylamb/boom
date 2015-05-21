package com.martiansoftware.boom;

import com.martiansoftware.dumbtemplates.DumbTemplate;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
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
import spark.SparkBase;
import spark.TemplateEngine;
import spark.TemplateViewRoute;
import spark.route.HttpMethod;

/**
 *
 * @author mlamb
 */
public class Boom extends SparkBase {

    private Boom() {}
    private static final Logger log = LoggerFactory.getLogger(Boom.class);
    private static final boolean _debug;
    
    private static final ThreadLocal<BoomContext> _context = new ThreadLocal<>();
    
    private static ContextFactory _templateContextFactory = java.util.HashMap::new;

    private static final DumbTemplateStore _templates;
    private static final PathResolver _pathResolver = new PathResolver("/");
    
    static {
        // TODO: allow port and static content to be done before routes are added.
        initStaticContent();
        initThreadLocalsFilter();
        _debug = Debug.init();
        _templates = Templates.init();
    }
    
    /**
     * Provides an external entrypoint that can trigger Boom's static initializer
     * if needed
     */
    public static void init() {}
    
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
    public static Request request() { return _context.get().request; }

    /**
     * Returns the Spark Response that is currently being serviced
     * @return the Spark Response that is currently being serviced
    */
    public static Response response() { return _context.get().response; }
    
    public static String resolvePath(String path) { return _pathResolver.resolve(path).toString(); }

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
     * Specify a ContextFactory to provide a preinitialized or otherwise specialized context
     * @param cf the ContextFactory to use for all requests
     */
    public static void contextFactory(ContextFactory cf) { _templateContextFactory = cf; }
    
    public static ResourceBundle r(String bundleName) {
        ResourceBundle result = null;
        try { ResourceBundle.getBundle("bundles." + bundleName); } catch (MissingResourceException ohWell) {};
        if (result == null) result = ResourceBundle.getBundle("boom-default-bundles." + bundleName);
        return result;
    }
    
    /**
     * Returns the working template context
     * @return the working template context
     */
    public static Map<String, Object> context() { return _context.get().templateContext; }

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
    
    public static void before(Filter filter) { Spark.before(filter); }
    public static void before(String path, Filter filter) { Spark.before(path, filter); }
    public static void before(String path, String acceptType, Filter filter) { Spark.before(path, acceptType, filter); }
    
    public static void after(Filter filter) { Spark.after(filter); }
    public static void after(String path, Filter filter) { Spark.after(path, filter); }
    public static void after(String path, String acceptType, Filter filter) { Spark.after(path, acceptType, filter); }
    
    
    protected static Route boomwrap(final Route route) {
        return new RouteWrapper(route);
    }
    
    protected static TemplateViewRoute boomwrap(TemplateViewRoute route) {
        return route;
    }
    
    
    // below methods set up all the static boom fun stuff
    static void initStaticContent() {
        if (debug()) {
            externalStaticFileLocation(Constants.STATIC_CONTENT_DEBUG);
        } else {
            staticFileLocation(Constants.STATIC_CONTENT_PRODUCTION);
        }
    }

    private static void initThreadLocalsFilter() {
        Spark.before((Request req, Response rsp) -> {
            Map<String, Object> tctx = _templateContextFactory.createContext();
            tctx.put(Constants.BOOM_ROOT, _pathResolver.resolve("/"));
            _context.set(new BoomContext(req, rsp, tctx));            
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
        
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        log.debug("Adding route for {} {} from {}", method, path, stack[3]);
    }
    
    // a whole bunch of convenient methods for creating BoomResponses of various typs
    public static BoomResponse binary(InputStream in) { return new BoomResponse(in).as(MimeType.BIN); }
    public static BoomResponse binary(File f) throws IOException { return new BoomResponse(f).as(MimeType.BIN); }
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
