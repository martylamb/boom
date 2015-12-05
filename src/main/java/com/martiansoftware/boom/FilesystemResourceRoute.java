package com.martiansoftware.boom;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public class FilesystemResourceRoute extends ResourceRoute {
    
    private static final Logger log = LoggerFactory.getLogger(FilesystemResourceRoute.class);
    
    private final Path _prefix;
    
    public FilesystemResourceRoute() { this(null, null); }
    public FilesystemResourceRoute(String prefix) { this(prefix, null); }
    public FilesystemResourceRoute(ResourceRoute next) { this(null, next); }
    
    public FilesystemResourceRoute(String prefix, ResourceRoute next) {
        super(next);
        _prefix = Paths.get(prefix);
    }

    @Override InputStream getInputStream(String path) {
        if (_prefix != null) path = _prefix.resolve(path.replaceAll("^/*", "")).toString();
        log.debug("Looking for [{}]", path);

        Path p = Paths.get(path);
        if (Files.isDirectory(p)) {
            log.debug("Not resolving path that points to a directory: {}", path);
        } else if (Files.isRegularFile(p)) {
            try {
                return new BufferedInputStream(Files.newInputStream(p));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }
    
}
