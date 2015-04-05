package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.*;
import com.martiansoftware.dumbtemplates.DumbLazyClasspathTemplateStore;
import com.martiansoftware.dumbtemplates.DumbTemplateStore;

class _InitDebugUi {

    private static final String DEBUG_URL_PREFIX = "/boom/";
    private static final String DEBUG_CP_STATIC_RESOURCES = "/boom-debug-static-content";
    private static final DumbTemplateStore _templates = new DumbLazyClasspathTemplateStore("/boom-debug-templates");
    
    public static void init() {
        if (!Boom.debug()) return;
        getDebugResource("pure-min.css");
        getDebugResource("jquery-2.1.3.min.js");
        get(urlOf(""), () -> doIndex());
        get(urlOf("oops"), () -> { throw new Exception("OH NOES!"); });
    }

    private static Object doIndex() {
        return _templates.get("index.html").render(null);
    }
    
    private static void getDebugResource(String resource) {
        get(urlOf(resource), () -> {
            return new ResponseHelper(_InitDebugUi.class.getResource(DEBUG_CP_STATIC_RESOURCES + "/" + resource));
        });
    }

    private static String urlOf(String s) { return DEBUG_URL_PREFIX + s; }    
}
