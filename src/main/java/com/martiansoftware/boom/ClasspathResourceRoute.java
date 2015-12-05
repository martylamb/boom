package com.martiansoftware.boom;

import java.io.File;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public class ClasspathResourceRoute extends ResourceRoute {

    private static final Logger log = LoggerFactory.getLogger(ClasspathResourceRoute.class);
    
    private final Path _prefix;
    
    public ClasspathResourceRoute() {
        this(null, null);
    }
    
    public ClasspathResourceRoute(ResourceRoute next) {
        this(null, next);
    }
    
    public ClasspathResourceRoute(String cpPrefix) {
        this(cpPrefix, null);
    }
    
    public ClasspathResourceRoute(String cpPrefix, ResourceRoute next) {
        super(next);
        _prefix = Paths.get(cpPrefix);
    }
    
    @Override InputStream getInputStream(String path) {
        if (_prefix != null) path = _prefix.resolve(path.replaceAll("^/*", "")).toString();
        log.debug("Looking for [{}]", path);
        
        URL url = this.getClass().getResource(path);
        if ("file".equals(url.getProtocol())) {
            try {
                File f = new File(url.toURI());
                if (f.isDirectory()) {
                    // classloader.getResourceAsStream will return a directory listing - don't want that
                    log.debug("Not resolving path that points to a directory: {}", path);
                    return null;
                }
            } catch (URISyntaxException e) {
                log.error(e.getMessage(), e);
            }
        }

        FakeInputStreamDetector result = new FakeInputStreamDetector(this.getClass().getResourceAsStream(path));
        return result.isReadable() ? result : null;
    }
    
    class FakeInputStreamDetector extends PushbackInputStream {

        private final boolean _readable;

        // if you call getResourceAsStream() against a directory in a jar, you apparently get
        // a non-null InputStream that throws a NullPointerException as soon as you try to
        // read from it.  can't imagine who thought that would be helpful, but this works around
        // that to see if the resource is truly readable by trying to read from it.
        public FakeInputStreamDetector(InputStream in) {
            super(in, 1);
            boolean readable = false;
            if (in != null) {
                try {                
                    int i = in.read();
                    if (i >= 0) unread(i);
                    readable = true;
                } catch (NullPointerException sometimesExpected) {
                    log.trace("NPE on trial read");
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            _readable = readable;
        }
        
        public boolean isReadable() { return _readable; }
        
    }
}
