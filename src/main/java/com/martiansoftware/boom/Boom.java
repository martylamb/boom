package com.martiansoftware.boom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.martiansoftware.dumbtemplates.DumbTemplate;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.util.Map;
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

/**
 *
 * @author mlamb
 */
public class Boom extends SparkBase {

    private Boom() {}
    private static final Logger log = LoggerFactory.getLogger(Boom.class);
    private static final boolean _debug;
    
    private static final ThreadLocal<Request> _request = new ThreadLocal<>();
    private static final ThreadLocal<Response> _response = new ThreadLocal<>();
    
    private static ContextFactory _contextFactory = java.util.HashMap::new;

    private static final ThreadLocal<Map<String, Object>> _context = new ThreadLocal<>();
    
    private static final DumbTemplateStore _templates;
    
    static {
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
    public static Request request() { return _request.get(); }

    /**
     * Returns the Spark Response that is currently being serviced
     * @return the Spark Response that is currently being serviced
    */
    public static Response response() { return _response.get(); }
    
    /**
     * Specify a ContextFactory to provide a preinitialized or otherwise specialized context
     * @param cf the ContextFactory to use for all requests
     */
    public static void contextFactory(ContextFactory cf) { _contextFactory = cf; }
    
    /**
     * Returns the working template context
     * @return the working template context
     */
    public static Map<String, Object> context() { return _context.get(); }

    // simple redirect shorthand - makes it easier to redirect from a simpleroute
    public static Object redirect(String location, int statusCode) { response().redirect(location, statusCode); return null; }
    public static Object redirect(String location) { response().redirect(location); return null; }
    
    // basic template accessors
    public static DumbTemplate template(String templatePath) { return _templates.get(templatePath); }
    public static DumbTemplateStore templateStore() { return _templates; }

    // TODO: client-side sessions
    public static Session session(boolean create) { return request().session(create); }
    public static Session session() { return session(true); }

    // straight passthrough to Spark methods
    public static ModelAndView modelAndView(Object model, String viewName) { return Spark.modelAndView(model, viewName); }
    public static void halt() { Spark.halt(); }
    public static void halt(int status) { Spark.halt(status); }
    public static void halt(int status, String body) { Spark.halt(status, body); }
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
            externalStaticFileLocation("src/main/resources/static-content");
        } else {
            staticFileLocation("/static-content");
        }
    }

    private static void initThreadLocalsFilter() {
        Spark.before((Request req, Response rsp) -> {
            _request.set(req);
            _response.set(rsp);
            _context.set(new java.util.HashMap<>());
        });        
    }
    
    public interface ContextFactory {
        public Map<String, Object> createContext();
    }
    
// the below is created by scripts/updateBoomJava    
// ## BEGIN GENERATED CODE - DO NOT EDIT BELOW THIS LINE ##
    public static synchronized void connect(final String path, final Route route) {
        Spark.connect(path, boomwrap(route));
    }
    
    public static synchronized void connect(final String path, final BoomRoute route) {
        Spark.connect(path, boomwrap(route));
    }
    
    public static synchronized void connect(String path, Route route, ResponseTransformer transformer) {
        Spark.connect(path, boomwrap(route), transformer);
    }
    
    public static synchronized void connect(String path, String acceptType, Route route) {
        Spark.connect(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void connect(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.connect(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void connect(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.connect(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void connect(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.connect(path, boomwrap(route), engine);
    }

    public static synchronized void delete(final String path, final Route route) {
        Spark.delete(path, boomwrap(route));
    }
    
    public static synchronized void delete(final String path, final BoomRoute route) {
        Spark.delete(path, boomwrap(route));
    }
    
    public static synchronized void delete(String path, Route route, ResponseTransformer transformer) {
        Spark.delete(path, boomwrap(route), transformer);
    }
    
    public static synchronized void delete(String path, String acceptType, Route route) {
        Spark.delete(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void delete(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.delete(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void delete(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.delete(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void delete(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.delete(path, boomwrap(route), engine);
    }

    public static synchronized void get(final String path, final Route route) {
        Spark.get(path, boomwrap(route));
    }
    
    public static synchronized void get(final String path, final BoomRoute route) {
        Spark.get(path, boomwrap(route));
    }
    
    public static synchronized void get(String path, Route route, ResponseTransformer transformer) {
        Spark.get(path, boomwrap(route), transformer);
    }
    
    public static synchronized void get(String path, String acceptType, Route route) {
        Spark.get(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void get(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.get(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void get(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.get(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void get(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.get(path, boomwrap(route), engine);
    }

    public static synchronized void head(final String path, final Route route) {
        Spark.head(path, boomwrap(route));
    }
    
    public static synchronized void head(final String path, final BoomRoute route) {
        Spark.head(path, boomwrap(route));
    }
    
    public static synchronized void head(String path, Route route, ResponseTransformer transformer) {
        Spark.head(path, boomwrap(route), transformer);
    }
    
    public static synchronized void head(String path, String acceptType, Route route) {
        Spark.head(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void head(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.head(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void head(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.head(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void head(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.head(path, boomwrap(route), engine);
    }

    public static synchronized void options(final String path, final Route route) {
        Spark.options(path, boomwrap(route));
    }
    
    public static synchronized void options(final String path, final BoomRoute route) {
        Spark.options(path, boomwrap(route));
    }
    
    public static synchronized void options(String path, Route route, ResponseTransformer transformer) {
        Spark.options(path, boomwrap(route), transformer);
    }
    
    public static synchronized void options(String path, String acceptType, Route route) {
        Spark.options(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void options(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.options(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void options(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.options(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void options(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.options(path, boomwrap(route), engine);
    }

    public static synchronized void patch(final String path, final Route route) {
        Spark.patch(path, boomwrap(route));
    }
    
    public static synchronized void patch(final String path, final BoomRoute route) {
        Spark.patch(path, boomwrap(route));
    }
    
    public static synchronized void patch(String path, Route route, ResponseTransformer transformer) {
        Spark.patch(path, boomwrap(route), transformer);
    }
    
    public static synchronized void patch(String path, String acceptType, Route route) {
        Spark.patch(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void patch(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.patch(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void patch(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.patch(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void patch(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.patch(path, boomwrap(route), engine);
    }

    public static synchronized void post(final String path, final Route route) {
        Spark.post(path, boomwrap(route));
    }
    
    public static synchronized void post(final String path, final BoomRoute route) {
        Spark.post(path, boomwrap(route));
    }
    
    public static synchronized void post(String path, Route route, ResponseTransformer transformer) {
        Spark.post(path, boomwrap(route), transformer);
    }
    
    public static synchronized void post(String path, String acceptType, Route route) {
        Spark.post(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void post(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.post(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void post(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.post(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void post(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.post(path, boomwrap(route), engine);
    }

    public static synchronized void put(final String path, final Route route) {
        Spark.put(path, boomwrap(route));
    }
    
    public static synchronized void put(final String path, final BoomRoute route) {
        Spark.put(path, boomwrap(route));
    }
    
    public static synchronized void put(String path, Route route, ResponseTransformer transformer) {
        Spark.put(path, boomwrap(route), transformer);
    }
    
    public static synchronized void put(String path, String acceptType, Route route) {
        Spark.put(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void put(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.put(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void put(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.put(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void put(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.put(path, boomwrap(route), engine);
    }

    public static synchronized void trace(final String path, final Route route) {
        Spark.trace(path, boomwrap(route));
    }
    
    public static synchronized void trace(final String path, final BoomRoute route) {
        Spark.trace(path, boomwrap(route));
    }
    
    public static synchronized void trace(String path, Route route, ResponseTransformer transformer) {
        Spark.trace(path, boomwrap(route), transformer);
    }
    
    public static synchronized void trace(String path, String acceptType, Route route) {
        Spark.trace(path, acceptType, boomwrap(route));
    }
    
    public static synchronized void trace(String path, String acceptType, Route route, ResponseTransformer transformer) {
        Spark.trace(path, acceptType, boomwrap(route), transformer);
    }
    
    public static synchronized void trace(String path, String acceptType, TemplateViewRoute route, TemplateEngine engine) {
        Spark.trace(path, acceptType, boomwrap(route), engine);
    }
    
    public static synchronized void trace(String path, TemplateViewRoute route, TemplateEngine engine) {
        Spark.trace(path, boomwrap(route), engine);
    }

}
