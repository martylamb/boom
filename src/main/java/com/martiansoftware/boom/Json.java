/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martiansoftware.boom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.martiansoftware.io.AtomicFileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author mlamb
 */
public class Json {
    private static Gson _gson;
    
    private static Gson gson() {
        if (_gson == null) _gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssX").setPrettyPrinting().create();
        return _gson;
    }

    public static String toJson(Object src) { return gson().toJson(src); }
    public static void toJson(Object src, Appendable writer) { gson().toJson(src, writer); }
    public static void toJson(Object src, Path p) throws IOException {
        AtomicFileOutputStream ao = new AtomicFileOutputStream(p);
        OutputStreamWriter o = new OutputStreamWriter(ao);
        boolean success = false;
        try {
            gson().toJson(src, o);
            o.close();
            success = true;
        } finally {
            if (!success) ao.cancel();
        }
    }
    
    public static <T> T fromJson(String j, Class<T> clazz) { return gson().fromJson(j, clazz); }
    public static <T> T fromJson(Reader r, Class<T> clazz) { return gson().fromJson(r, clazz); }    
    public static <T> T fromJson(Path p, Class<T> clazz) throws IOException {
        try(Reader r = Files.newBufferedReader(p)) {
            return gson().fromJson(r, clazz);
        }
    }
    
}
