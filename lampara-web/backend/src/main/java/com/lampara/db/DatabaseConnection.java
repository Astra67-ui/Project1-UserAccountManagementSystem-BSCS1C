package com.lampara.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        try (InputStream in = DatabaseConnection.class
                .getResourceAsStream("/db.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                URL      = p.getProperty("db.url");
                USER     = p.getProperty("db.user");
                PASSWORD = p.getProperty("db.password");
            } else {
                // Fallback defaults (XAMPP)
                URL      = "jdbc:mysql://localhost:3306/lampara_db?useSSL=false&serverTimezone=UTC";
                USER     = "root";
                PASSWORD = "";
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Cannot load db.properties: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found.", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
