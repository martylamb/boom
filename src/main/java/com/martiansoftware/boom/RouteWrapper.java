package com.martiansoftware.boom;

import java.util.Map;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Route;

import static com.martiansoftware.boom.Boom.*;
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
        Object result = null;
        try {
            result = _wrapped.handle(rqst, rspns);
            if (result instanceof BoomResponse) {
                result = ((BoomResponse) result).respond(rspns);
            }
            return result;
        } catch (Exception e) {
            if (Boom.debug()) {
                Map<String, Object> context = new java.util.HashMap<>();
                int code = 500;
                String msg = e.getMessage();

                if(e instanceof HaltException) {
                    HaltException he = (HaltException) e;
                    code = he.getStatusCode();
                    msg = he.getBody();
                }
                
                context.put("code", code);
                context.put("msg", msg);
                BoomResponse rh = new BoomResponse(template("/boom/status/status.html").render(context))
                                        .status(code)
                                        .mimeType(MimeType.HTML);
                
                return rh.respond(rspns);
            } else {
                throw e;
            }
        }
    }
    
}
