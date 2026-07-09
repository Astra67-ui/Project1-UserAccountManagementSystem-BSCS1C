package com.lampara;

import com.lampara.api.AuthHandler;
import com.lampara.api.UserHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Auth routes
        server.createContext("/api/register", new AuthHandler());
        server.createContext("/api/login",    new AuthHandler());
        server.createContext("/api/logout",   new AuthHandler());

        // OTP routes
        server.createContext("/api/verify-otp", new AuthHandler());
        server.createContext("/api/resend-otp", new AuthHandler());

        // User (protected) routes
        server.createContext("/api/user/profile",  new UserHandler());
        server.createContext("/api/user/password", new UserHandler());
        server.createContext("/api/user/account",  new UserHandler());

        // Admin routes
        server.createContext("/api/admin", new UserHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   LAMPARA Backend running on port " + port + " ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("API base: http://localhost:" + port + "/api");
        System.out.println("You may now proceed to run index.html.");
    }
}
