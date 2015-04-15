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
    
    public BoomResponse body(InputStream in) { bodyStream = in; bodyString = null; return this; }
    public BoomResponse body(String s) { bodyStream = null; bodyString = s; return this; }
    public BoomResponse body(File f) throws IOException { 
        bodyStream = new BufferedInputStream(new FileInputStream(f));
        bodyString = null;
        mimeType(MimeType.forFilename(f.getName()));
        return this;
    }
    public BoomResponse body(URL url) throws IOException {
        bodyStream = url.openStream();
        bodyString = null;
        mimeType(MimeType.forFilename(url.getFile().replaceAll(".*/", "")));
        return this;
    }
    
    public BoomResponse status(int statusCode) { status = statusCode; return this; }
    public BoomResponse ok() { return status(HttpServletResponse.SC_OK); }
    public BoomResponse forbidden() { return status(HttpServletResponse.SC_FORBIDDEN); }
    public BoomResponse error() { return status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); }
    public BoomResponse notFound() { return status(HttpServletResponse.SC_NOT_FOUND); }
    public BoomResponse unauthorized() { return status(HttpServletResponse.SC_UNAUTHORIZED); }
    
    public BoomResponse mimeType(String type) { mimeType = type; return this; }
    public BoomResponse mimeType(MimeType type) { mimeType = type.toString(); return this; }
    
    // simple helpers for fluent assignment of common mime types
    public BoomResponse binary() { return mimeType(MimeType.BIN); }
    public BoomResponse javascript() { return mimeType(MimeType.JS); }
    public BoomResponse css() { return mimeType(MimeType.CSS); }
    public BoomResponse html() { return mimeType(MimeType.HTML); }
    public BoomResponse text() { return mimeType(MimeType.TXT); }
    public BoomResponse csv() { return mimeType(MimeType.CSV); }
    public BoomResponse xml() { return mimeType(MimeType.XML); }
    public BoomResponse json() { return mimeType(MimeType.JSON); }
    
    // compound helpers for concise common use cases
    public BoomResponse html(String html) { body(html); return html(); }
    public BoomResponse json(String json) { body(json); return json(); }
    public BoomResponse json(Object jobj) { body(Json.toJson(jobj)); return json(); }
    
    public BoomResponse text(String text) { body(text); return text(); }
    public BoomResponse xml(String xml) { body(xml); return xml(); }
    public BoomResponse binary(byte[] b) { body(new ByteArrayInputStream(b)); return binary(); }
    public BoomResponse binary(byte[] b, int offset, int len) { body(new ByteArrayInputStream(b, offset, len)); return binary(); }
    
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
