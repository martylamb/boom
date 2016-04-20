package com.martiansoftware.boom;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mlamb
 */
public class PathResolver {

    private static final Logger log = LoggerFactory.getLogger(PathResolver.class);
    
    private static final Path TOP = Paths.get("/");
    private final Path _appRoot;
    
    public PathResolver(String appRoot) { this(safePath(appRoot)); }
    public PathResolver(Path appRoot) { _appRoot = collapseToAbsolute(appRoot); }
    
    public String resolve(String p) { return resolve(safePath(p)); }    
    public String resolve(Path p) { return _appRoot.resolve(collapseToRelative(safePath(p))).toString(); }

    private Path collapseToAbsolute(Path pathToCollapse) {
        return TOP.resolve(safePath(pathToCollapse).normalize());
    }
    
    private Path collapseToRelative(Path pathToCollapse) {
        return Paths.get(pathToCollapse.normalize().toString().replaceAll("^/+", ""));
    }
    
    private static Path safePath(String s) {
        if (s == null || s.isEmpty()) return TOP;
        return safePath(Paths.get(s));
    }
    
    private static Path safePath(Path p) {
        if (p == null || p.toString().isEmpty()) return TOP;
        return p;
    }
    
    public static void main(String[] args) {
        PathResolver pr = new PathResolver("myapp/..//////////something//");
        log.info(pr.resolve("//////////////.//../../../bookmarks//a/b/somewhere/../c"));
        log.info(pr.resolve((String) null));
        log.info(pr.resolve((Path) null));
        log.info(pr.resolve(""));
        log.info(pr.resolve("/"));
    }
}
