package com.mediinsight.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MediInsightApplication {
    public static void main(String[] args) {
        // Help JNA locate native libtesseract library on Apple Silicon Macs
        String existingJnaPath = System.getProperty("jna.library.path", "");
        String homebrewLibPath = "/opt/homebrew/lib";
        if (new java.io.File(homebrewLibPath).exists()) {
            if (existingJnaPath.isEmpty()) {
                System.setProperty("jna.library.path", homebrewLibPath);
            } else {
                System.setProperty("jna.library.path", existingJnaPath + ":" + homebrewLibPath);
            }
        }
        SpringApplication.run(MediInsightApplication.class, args);
    }
}
