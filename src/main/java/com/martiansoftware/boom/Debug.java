package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.*;
import com.martiansoftware.dumbtemplates.DumbLazyClasspathTemplateStore;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

class Debug {

    private static final String DEBUG_URL_PREFIX = "/boom-debug";
    private static final String DEBUG_CP_STATIC_RESOURCES = "/boom-debug-static-content";
    
    private static final Logger log = LoggerFactory.getLogger(Debug.class);
    
    private static final DumbTemplateStore _templates = new DumbLazyClasspathTemplateStore("/boom-debug-templates");
    
    static boolean init() {
        // can be set by environment var or system property.  system property setting wins if set.
        String d = System.getProperty("BOOM_DEBUG", System.getenv("BOOM_DEBUG"));
        boolean result = (d != null && d.trim().equals("1"));
        if (result) {
            log.warn("*****************************");
            log.warn("*** RUNNING IN DEBUG MODE ***");
            log.warn("*****************************");

            log.warn("Exposing debug UI at {}", DEBUG_URL_PREFIX);

            // messy but simple way to serve static resources needed for debug ui
            Arrays.stream("jquery-2.1.3.min.js jquery.dataTables.min.css jquery.dataTables.min.js pure-min.css sort_asc_disabled.png sort_asc.png sort_both.png sort_desc_disabled.png sort_desc.png".split("\\s+"))
                .forEach((dr) -> getDebugResource(dr));

            // do the same thing whether there's a trailing slash or not
            get(DEBUG_URL_PREFIX, () -> redirect(DEBUG_URL_PREFIX + "/"));
            get(DEBUG_URL_PREFIX + "/", () -> doIndex());
        
        }
        return result;
    }
    
    private static Object doIndex() {
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
    
    private static void getDebugResource(String resource) {
        get(urlOf(resource), () -> {
            return new ResponseHelper(Debug.class.getResource(DEBUG_CP_STATIC_RESOURCES + "/" + resource));
        });
    }

    private static String urlOf(String s) { return DEBUG_URL_PREFIX + "/" + s; }    
    
    // helper class to fill jquery datatable on client side
    private static class DataTable {
        public String c1, c2;
        public List<String[]> rows = new java.util.LinkedList<>();
        public DataTable(String c1, String c2) { this.c1 = c1; this.c2 = c2; }
        public void add(String k, String v) { rows.add(new String[]{k,v}); }
    }
}
