package com.lampara.util;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
 
public class HttpUtil {

    // ── Read request body as String ──────────────────────────────────────
    public static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ── Send JSON response ───────────────────────────────────────────────
    public static void sendJson(HttpExchange ex, int status, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCorsHeaders(ex);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ── Send simple success/error JSON ───────────────────────────────────
    public static void sendOk(HttpExchange ex, String message) throws IOException {
        sendJson(ex, 200, new JSONObject().put("success", true).put("message", message));
    }

    public static void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, new JSONObject().put("success", false).put("message", message));
    }

    // ── CORS preflight ───────────────────────────────────────────────────
    public static void addCorsHeaders(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    public static boolean handleOptions(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            addCorsHeaders(ex);
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // ── Parse query string ────────────────────────────────────────────────
    public static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}
