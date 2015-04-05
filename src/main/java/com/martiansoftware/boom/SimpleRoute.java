package com.martiansoftware.boom;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 *
 * @author mlamb
 */
public interface SimpleRoute extends Route {
    
    @Override public default Object handle(Request request, Response response) throws Exception {
        return handle();
    }
    
    public Object handle() throws Exception;
}
