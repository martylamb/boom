package com.martiansoftware.boom;

import java.util.Map;
import spark.Request;
import spark.Response;

/**
 *
 * @author mlamb
 */
class BoomContext {
    public final Request request;
    public final Response response;
    public final Map<String, Object> templateContext;
    
    BoomContext(Request _request, Response _response, Map<String, Object> _templateContext) {
        request = _request;
        response = _response;
        templateContext = _templateContext;
    }
    
}
