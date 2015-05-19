package com.martiansoftware.boom;

import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Route;

/**
 *
 * @author mlamb
 */
class RouteWrapper implements Route {

    private final Route _wrapped;
    
    RouteWrapper(Route wrapped) {
        _wrapped = wrapped;
    }
    
    @Override
    public Object handle(Request rqst, Response rspns) throws Exception {
        try {
            Object result = _wrapped.handle(rqst, rspns);
            if (result instanceof BoomResponse) {
                result = ((BoomResponse) result).respond(rspns);
            }
            return result;
        } catch (HaltException he) {
            return StatusPage.of(he).respond(rspns);
        } catch (Exception e) {
            return StatusPage.of(e).respond(rspns);
        }
    }
    
}
