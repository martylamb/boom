package com.martiansoftware.boom;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

/**
 *
 * @author mlamb
 */
class BoomContext {
    private static final Logger log = LoggerFactory.getLogger(BoomContext.class);
    
    public final Request request;
    public final Response response;
    public final Map<String, Object> templateContext;
    private Path tmp = null;    
    
    BoomContext(Request _request, Response _response, Map<String, Object> _templateContext) {
        request = _request;
        response = _response;
        templateContext = _templateContext;
    }
    
    public synchronized Path tmp() throws IOException {
        if (tmp == null) {
            tmp = Files.createTempDirectory(Boom._tmp, "boom-");
        }
        return tmp;
    }
    
    void cleanup() {
        log.debug("Cleaning up.");
        if (tmp != null && Files.exists(tmp)) {
            try {
                // TODO: pull this out to an API
                Files.walkFileTree(tmp, new SimpleFileVisitor<Path>() {
                    @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        log.warn("Deleting {}", file);
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        log.warn("Deleting {}", dir);
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.error("Error while cleaning up BoomContext: " + e.getMessage(), e);
            }
        }
    }
    
}
