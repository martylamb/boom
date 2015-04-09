package com.martiansoftware.boom;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.servlet.http.HttpServletResponse;
import spark.Response;

/**
 *
 * @author mlamb
 */
public class ResponseHelper {

    private int status = HttpServletResponse.SC_OK;
    private InputStream bodyStream;
    private String bodyString;
    private String mimeType = MimeType.HTML.toString();
    
    public ResponseHelper() { this(""); }
    public ResponseHelper(InputStream in) { body(in); }
    public ResponseHelper(String s) { body(s); }
    public ResponseHelper(File f) throws IOException { body(f); }
    public ResponseHelper(URL url) throws IOException { body(url); }
    
    public ResponseHelper body(InputStream in) { bodyStream = in; bodyString = null; return this; }
    public ResponseHelper body(String s) { bodyStream = null; bodyString = s; return this; }
    public ResponseHelper body(File f) throws IOException { 
        bodyStream = new BufferedInputStream(new FileInputStream(f));
        bodyString = null;
        mimeType(MimeType.forFilename(f.getName()));
        return this;
    }
    public ResponseHelper body(URL url) throws IOException {
        bodyStream = url.openStream();
        bodyString = null;
        mimeType(MimeType.forFilename(url.getFile().replaceAll(".*/", "")));
        return this;
    }
    
    public ResponseHelper status(int statusCode) { status = statusCode; return this; }
    public ResponseHelper ok() { return status(HttpServletResponse.SC_OK); }
    public ResponseHelper forbidden() { return status(HttpServletResponse.SC_FORBIDDEN); }
    public ResponseHelper error() { return status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }
    public ResponseHelper notFound() { return status(HttpServletResponse.SC_NOT_FOUND); }
    public ResponseHelper unauthorized() { return status(HttpServletResponse.SC_UNAUTHORIZED); }
    
    public ResponseHelper mimeType(String type) { mimeType = type; return this; }
    public ResponseHelper mimeType(MimeType type) { mimeType = type.toString(); return this; }
    
    // simple helpers for fluent assignment of common mime types
    public ResponseHelper binary() { return mimeType(MimeType.BIN); }
    public ResponseHelper javascript() { return mimeType(MimeType.JS); }
    public ResponseHelper css() { return mimeType(MimeType.CSS); }
    public ResponseHelper html() { return mimeType(MimeType.HTML); }
    public ResponseHelper text() { return mimeType(MimeType.TXT); }
    public ResponseHelper csv() { return mimeType(MimeType.CSV); }
    public ResponseHelper xml() { return mimeType(MimeType.XML); }
    public ResponseHelper json() { return mimeType(MimeType.JSON); }
    
    // compound helpers for concise common use cases
    public ResponseHelper html(String html) { body(html); return html(); }
    public ResponseHelper json(String json) { body(json); return json(); }
    public ResponseHelper json(Object jobj) { body(Boom.json(jobj)); return json(); }
    
    public ResponseHelper text(String text) { body(text); return text(); }
    public ResponseHelper xml(String xml) { body(xml); return xml(); }
    public ResponseHelper binary(byte[] b) { body(new ByteArrayInputStream(b)); return binary(); }
    public ResponseHelper binary(byte[] b, int offset, int len) { body(new ByteArrayInputStream(b, offset, len)); return binary(); }
    
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
        StringBuilder sb = new StringBuilder("ResponseHelper: ");
        if (bodyStream != null) sb.append("(InputStream) ");
        else sb.append("(String) " );
        sb.append("type: ");
        sb.append(mimeType);
        sb.append(" status: ");
        sb.append(status);
        return sb.toString();
    }
            
}
