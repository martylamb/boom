package com.martiansoftware.boom;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;

/**
 *
 * @author mlamb
 */
public class PathResolver {

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
        System.out.println(pr.resolve("//////////////.//../../../bookmarks//a/b/somewhere/../c"));
        System.out.println(pr.resolve((String) null));
        System.out.println(pr.resolve((Path) null));
        System.out.println(pr.resolve(""));
        System.out.println(pr.resolve("/"));
    }
}
