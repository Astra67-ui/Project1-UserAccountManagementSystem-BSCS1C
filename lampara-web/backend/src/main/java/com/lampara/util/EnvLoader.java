package com.lampara.util;

import java.io.*;
import java.nio.file.*;

public class EnvLoader {
    public static void load(String path) {
        try {
            for (String line : Files.readAllLines(Paths.get(path))) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int idx = line.indexOf('=');
                if (idx == -1) continue;
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                System.setProperty(key, value);
            }
        } catch (IOException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
    }
}