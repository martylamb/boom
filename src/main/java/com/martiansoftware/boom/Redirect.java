package com.martiansoftware.boom;

import spark.Route;

/**
 * Super simple redirection helper.
 * @author mlamb
 */
public class Redirect {
    public static Route to(String dest) {
        return (req,rsp) -> {
            rsp.redirect(dest);
            return null;
        };
    }
}
