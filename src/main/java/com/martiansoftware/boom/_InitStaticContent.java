package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.*;

public class _InitStaticContent {

    public static void init() {
        if (debug()) {
            externalStaticFileLocation("src/main/resources/static-content");
        } else {
            staticFileLocation("/static-content");
        }        
    }
}
