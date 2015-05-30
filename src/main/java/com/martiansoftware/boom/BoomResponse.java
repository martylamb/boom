package com.martiansoftware.boom;

import com.martiansoftware.dumbtemplates.DumbTemplate;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import spark.Response;
import static com.martiansoftware.boom.Boom.context;

/**
 *
 * @author mlamb
 */
public class BoomResponse {

    private int status = HttpServletResponse.SC_OK;
    private InputStream bodyStream;
    private String bodyString;
    private String mimeType = MimeType.HTML.toString();
    
    public BoomResponse() { this(""); }
    public BoomResponse(InputStream in) { body(in); }
    public BoomResponse(String s) { body(s); }
    public BoomResponse(File f) throws IOException { body(f); }
    public BoomResponse(URL url) throws IOException { body(url); }
    public BoomResponse(DumbTemplate t) { body(t.render(context())); }
    
    public BoomResponse body(InputStream in) { bodyStream = in; bodyString = null; return this; }
    public BoomResponse body(String s) { bodyStream = null; bodyString = s; return this; }
    public BoomResponse body(File f) throws IOException { 
        bodyStream = new BufferedInputStream(new FileInputStream(f));
        bodyString = null;
        as(MimeType.forFilename(f.getName()));
        return this;
    }
    public BoomResponse body(URL url) throws IOException {
        bodyStream = url.openStream();
        bodyString = null;
        as(MimeType.forFilename(url.getFile().replaceAll(".*/", "")));
        return this;
    }
    
    public BoomResponse status(int statusCode) { status = statusCode; return this; }
    public BoomResponse ok() { return status(HttpServletResponse.SC_OK); }
    public BoomResponse forbidden() { return status(HttpServletResponse.SC_FORBIDDEN); }
    public BoomResponse error() { return status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }
    public BoomResponse notFound() { return status(HttpServletResponse.SC_NOT_FOUND); }
    public BoomResponse unauthorized() { return status(HttpServletResponse.SC_UNAUTHORIZED); }
    
    public BoomResponse as(String type) { mimeType = type; return this; }
    public BoomResponse as(MimeType type) { mimeType = type.toString(); return this; }
        
    Object respond(Response rsp) throws IOException {
        rsp.status(status);
        rsp.type(mimeType);
        if (bodyStream != null) {
            copyStream(bodyStream, rsp.raw().getOutputStream());
            bodyStream.close();
            rsp.raw().getOutputStream().close();
            return rsp.raw();
        } else {
            return bodyString;
        }
    }
    
    private void copyStream(InputStream from, OutputStream to) throws IOException {
        int len, bufsize = 8192;
        byte[] buf = new byte[bufsize];
        while ((len = from.read(buf)) != -1) to.write(buf, 0, len);
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getName());
        sb.append(": ");
        sb.append((bodyStream == null) ? "(String)" : "(InputStream)");
        sb.append(" type: ");
        sb.append(mimeType);
        sb.append(" status: ");
        sb.append(status);
        return sb.toString();
    }
            
}
