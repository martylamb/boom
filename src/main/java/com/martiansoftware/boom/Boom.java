package com.martiansoftware.boom;

import com.martiansoftware.dumbtemplates.DumbTemplate;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ExceptionHandler;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;
import spark.Route;
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
    private static final ThreadLocal<Map<String, Object>> _context = new ThreadLocal<>();
    
    private static final DumbTemplateStore _templates;

    static {
        _debug = initDebug();
        _InitStaticContent.init();
        
        Spark.before((Request req, Response rsp) -> {
            _request.set(req);
            _response.set(rsp);
            _context.set(new java.util.HashMap<>());
        });

        _templates = _InitTemplates.init();
        _InitDebugUi.init();
        
//        Spark.exception(HaltException.class, new ExceptionHandler(){
//
//            @Override
//            public void handle(Exception e, Request rqst, Response rspns) {
//                if (e instanceof HaltException) {
//                    HaltException he = (HaltException) e;
//                    Map<String, Object> context = new java.util.HashMap<>();
//
//                    context.put("code", he.getStatusCode());
//                    context.put("msg", he.getBody());
//                    rspns.status(he.getStatusCode());
//                    rspns.body(template("/boom/status/status.html").render(context));
//                } else {
//                    e.printStackTrace();
//                }
//            }
//        });
    }
    
    public static void init() {} // external entrypoing to trigger static initializer if needed
    
    public static boolean debug() { return _debug; }    
    public static Request request() { return _request.get(); }
    public static Response response() { return _response.get(); }
    
    public static DumbTemplate template(String templatePath) { return _templates.get(templatePath); }
    public static DumbTemplateStore templateStore() { return _templates; }

    // straight passthrough to Spark methods
    public static ModelAndView modelAndView(Object model, String viewName) { return Spark.modelAndView(model, viewName); }
    public static void halt() { Spark.halt(); }
    public static void halt(int status) { Spark.halt(status); }
    public static void halt(int status, String body) { Spark.halt(status, body); }
    public static void halt(String body) { Spark.halt(body); }
    public static synchronized void exception(Class<? extends Exception> exceptionClass, ExceptionHandler handler) { Spark.exception(exceptionClass, handler); }
    
    protected static Route boomwrap(final Route route) {
        return new BoomRoute(route);
    }
    
    protected static TemplateViewRoute boomwrap(TemplateViewRoute route) {
        return route;
    }
    
    private static boolean initDebug() {
        // can be set by environment var or system property.  system property setting wins if set.
        String d = System.getProperty("BOOM_DEBUG", System.getenv("BOOM_DEBUG"));
        boolean result = (d != null && d.trim().equals("1"));
        if (result) {
            log.warn("*****************************");
            log.warn("*** RUNNING IN DEBUG MODE ***");
            log.warn("*****************************");
        }
        return result;
    }
    
    public static void autostart(){}
    
// the below is created by scripts/updateBoomJava    
// ## BEGIN GENERATED CODE - DO NOT EDIT BELOW THIS LINE ##
    public static synchronized void connect(final String path, final Route route) {
        Spark.connect(path, boomwrap(route));
    }
    
    public static synchronized void connect(final String path, final SimpleRoute route) {
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
    
    public static synchronized void delete(final String path, final SimpleRoute route) {
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
    
    public static synchronized void get(final String path, final SimpleRoute route) {
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
    
    public static synchronized void head(final String path, final SimpleRoute route) {
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
    
    public static synchronized void options(final String path, final SimpleRoute route) {
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
    
    public static synchronized void patch(final String path, final SimpleRoute route) {
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
    
    public static synchronized void post(final String path, final SimpleRoute route) {
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
    
    public static synchronized void put(final String path, final SimpleRoute route) {
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
    
    public static synchronized void trace(final String path, final SimpleRoute route) {
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
