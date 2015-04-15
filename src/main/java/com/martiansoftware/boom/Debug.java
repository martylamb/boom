package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.*;
import com.martiansoftware.dumbtemplates.DumbLazyClasspathTemplateStore;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

class Debug {

    private static final String DEBUG_URL_PREFIX = "/boom-debug";
    private static final String DEBUG_CP_STATIC_RESOURCES = "/boom-debug-static-content";
    
    private static final Logger log = LoggerFactory.getLogger(Debug.class);
    
    private final DumbTemplateStore _templates = new DumbLazyClasspathTemplateStore("/boom-debug-templates");
    private final RRLog _rrLog = new RRLog();
    
    static boolean init() {
        // can be set by environment var or system property.  system property setting wins if set.
        String d = System.getProperty(Constants.DEBUG_ENV_OR_PROPERTY, System.getenv(Constants.DEBUG_ENV_OR_PROPERTY));
        boolean result = (d != null && d.trim().equals("1"));
        if (result) {
            log.warn("*****************************");
            log.warn("*** RUNNING IN DEBUG MODE ***");
            log.warn("*****************************");

            log.warn("Exposing debug UI at {}", DEBUG_URL_PREFIX);        
            new Debug().registerUI();
        }
        return result;
    }
    
    private Debug() {}
    
    private void registerUI() {
        // messy but simple way to serve static resources needed for debug ui
        Arrays.stream("jquery-2.1.3.min.js jquery.dataTables.min.css jquery.dataTables.min.js pure-min.css sort_asc_disabled.png sort_asc.png sort_both.png sort_desc_disabled.png sort_desc.png".split("\\s+"))
            .forEach((dr) -> getDebugResource(dr));

        // do the same thing whether there's a trailing slash or not
        get(DEBUG_URL_PREFIX, () -> redirect(DEBUG_URL_PREFIX + "/"));
        get(DEBUG_URL_PREFIX + "/", () -> doRequest());           
        get(DEBUG_URL_PREFIX + "/list", () -> list());
        
        spark.Spark.after((req,rsp) -> { _rrLog.add(new RR(req,rsp)); });
        
//        spark.Spark.after((req,rsp) -> {
//            
//            System.out.println("Returned " + rsp.raw().getStatus());
//            System.out.println()
//        });
    }
    
    private Object list() {
        Map<String, Object> context = new java.util.HashMap<>();
        _rrLog.update();
        context.put("reqrsp", _rrLog.list);
        return _templates.get("list.html").render(context);
    }
    
    private Object doRequest() {
        Request req = request();
        Map<String, Object> context = new java.util.HashMap<>();

        DataTable headers = new DataTable("Header", "Value");
        req.headers().forEach((s) -> headers.add(s, req.headers(s)));        
        context.put("headers", headers);
        
        DataTable queryParams = new DataTable("Parameter", "Value");
        req.queryParams().forEach(System.out::println);
        req.queryParams().forEach((s) -> queryParams.add(s, req.queryParams(s)));
        context.put("queryParams", queryParams);
        
        context.put("req.method", req.requestMethod());
        context.put("req.query", req.queryString());
        context.put("req.url", req.url());
        context.put("req.ip", req.ip());
        context.put("req.port", req.port());
        context.put("now", new java.util.Date());
        return _templates.get("index.html").render(context);
    }
    
    private void getDebugResource(String resource) {
        get(urlOf(resource), () -> {
            return new BoomResponse(Debug.class.getResource(DEBUG_CP_STATIC_RESOURCES + "/" + resource));
        });
    }

    private String urlOf(String s) { return DEBUG_URL_PREFIX + "/" + s; }    
    
    // helper class to fill jquery datatable on client side
    private class DataTable {
        public String c1, c2;
        public List<String[]> rows = new java.util.LinkedList<>();
        public DataTable(String c1, String c2) { this.c1 = c1; this.c2 = c2; }
        public void add(String k, String v) { rows.add(new String[]{k,v}); }
    }

    private class RRLog {
        public static final int MAX_SIZE = 32; // TODO: make configurable?  expire by age?
        private Map<UUID, RR> byUUID = new java.util.HashMap<>();
        private ArrayDeque<RR> list = new java.util.ArrayDeque(MAX_SIZE);
        private final Object lock = new Object();
        public int size() { synchronized(lock) { return list.size(); } }
        public void clear() { synchronized(lock) { byUUID.clear(); list.clear(); } }
        public void add(RR rr) {
            synchronized(lock) {
                while (list.size() >= MAX_SIZE) {
                    RR rm = list.removeLast();
                    byUUID.remove(rm.uuid);
                }
                list.addFirst(rr);
            }
        }
        public Collection<RR> update() { 
            synchronized(lock) { 
                list.stream().forEach((rr) -> rr.rsp.update()); 
                return new java.util.ArrayList<>(list); 
            }
        }
    }
    
    private class RR {
        final Date ts;
        final UUID uuid;
        final RequestLog req;
        final ResponseLog rsp;
        RR(Request req, Response rsp) {            
            ts = new Date();
            uuid = UUID.randomUUID();
            this.req = new RequestLog(req);
            this.rsp = new ResponseLog(rsp);
        }
        void setResponse(Response rsp) {
            this.rsp.update(rsp);
        }
    }
    
    private class RequestLog {
        final Set<String> headers;
        final Set<String> queryParams;
        final String method, url, ip;
        final Map<String, String> cookies;
        RequestLog(Request req) {
            headers = req.headers();
            queryParams = req.queryParams();
            method = req.requestMethod();
            url = req.url();
            ip = req.ip();
            cookies = req.cookies();
        }
    }
    
    private class ResponseLog {
        int status;
        String type;
        String body;
        transient Response rsp;
        ResponseLog(Response rsp) {
            this.rsp = rsp;
            update();
        }
        void update(Response rsp) {
            this.rsp = rsp;
            update();
        }
        private void update() {
            if (rsp != null) {
                status = rsp.raw().getStatus();
                type = rsp.raw().getContentType();
                body = rsp.body();
            }
        }
    }
}
