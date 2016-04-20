package com.martiansoftware.boom;

import static com.martiansoftware.boom.Boom.*;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public abstract class ResourceRoute implements BoomRoute {
    private static final Logger log = LoggerFactory.getLogger(ResourceRoute.class);
    
    private final ResourceRoute _next;
    
    // TODO: specify a path prefix that will be stripped during resolution
    
    public ResourceRoute() { this(null); }
    public ResourceRoute(ResourceRoute next) { _next = next; }

    abstract InputStream getInputStream(String path);
    
    private InputStream tryInputStream(String path) {
        InputStream result = getInputStream(path);
        if (result == null && _next != null) result = _next.getInputStream(path);
        return result;
    }
    
    Object getResource(String path) throws Exception {
        Path p = Paths.get(path);
        InputStream in = tryInputStream(path);
        if (in == null) {
            log.warn("NULL input stream for path [{}] - trying index!", path);
            p = p.resolve("index.html");
            in = tryInputStream(p.toString());
        } // FIXME: allow user to specify defaults to try (not just index.html)
        if (in == null) halt(HttpServletResponse.SC_NOT_FOUND);
        return new BoomResponse(in).as(MimeType.forPath(p));
    }
    
    @Override public Object handle() throws Exception {
        return getResource(request().pathInfo());
    }

}
