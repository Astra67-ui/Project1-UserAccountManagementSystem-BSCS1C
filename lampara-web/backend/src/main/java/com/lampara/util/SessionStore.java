package com.lampara.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {

    private static final Map<String, String> sessions = new ConcurrentHashMap<>(); // token → username
    private static final SecureRandom rng = new SecureRandom();

    public static String createSession(String username) {
        byte[] bytes = new byte[32];
        rng.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, username);
        return token;
    }

    public static String getUsernameForToken(String token) {
        return token == null ? null : sessions.get(token);
    }

    public static void invalidate(String token) {
        if (token != null) sessions.remove(token);
    }

    /** Extract Bearer token from Authorization header */
    public static String extractToken(com.sun.net.httpserver.HttpExchange ex) {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
