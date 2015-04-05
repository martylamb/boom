/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.martiansoftware.boomtest;

import com.martiansoftware.boom.Boom;

/**
 *
 * @author mlamb
 */
public class App {
 
    public static void main(String[] args) throws Exception {
        Boom.get("/", () -> "Hello.");
        
    }
}
