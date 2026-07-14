package com.lampara.db;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.lampara.model.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Contains all database operations 
public class UserStore {
 
    // Register a new user 
    public static void addUser(User u) throws SQLException {
        String hash = BCrypt.withDefaults().hashToString(12, u.password.toCharArray());
        String sql  = "INSERT INTO users (first_name,last_name,email,username,password, role) VALUES (?,?,?,?,?,?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.firstName);
            ps.setString(2, u.lastName);
            ps.setString(3, u.email);
            ps.setString(4, u.username);
            ps.setString(5, hash);
            ps.setString(6, "user");
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new SQLException("Username or email already taken.", dup);
        }
    }
 
    // Find user by username + verify password 
    public static User findUser(String username, String password) throws SQLException {
        String sql = "SELECT user_id,first_name,last_name,email,username,password,is_verified,role FROM users WHERE username=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password");
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hash);
                if (result.verified) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getBoolean("is_verified"),
                        rs.getString("role")
                    );
                }
            }
        }
        return null;
    }
 
    // Check if username exists 
    public static boolean usernameExists(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username=?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        }
    }
 
    //  Check if email exists
    public static boolean emailExists(String email) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE email=?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        }
    }
 
    //  Check if email is taken by another user 
    public static boolean isEmailTakenByOther(String newEmail, String currentEmail) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email=? AND email!=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setString(2, currentEmail);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
 
    //  Update profile 
    public static void updateUser(String username, String firstName, String lastName, String email) throws SQLException {
        String sql = "UPDATE users SET first_name=?,last_name=?,email=? WHERE username=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, username);
            ps.executeUpdate();
        }
    }
 
    // Change password 
    public static void updatePassword(String username, String newPassword) throws SQLException {
        String hash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        String sql  = "UPDATE users SET password=? WHERE username=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }
 
    //  Verify password 
    public static boolean verifyPassword(String username, String plain) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password FROM users WHERE username=?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return BCrypt.verifyer().verify(plain.toCharArray(), rs.getString("password")).verified;
            }
        }
        return false;
    }
 
    // Delete account 
    public static void removeUser(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE username=?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
 
    //  Mark email verified 
    public static void markVerified(String username) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET is_verified=TRUE WHERE username=?")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }
 
    // Get user by username (no password check) 
    public static User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, first_name, last_name, email, username, password, is_verified, role FROM users WHERE username=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getBoolean("is_verified"),
                    rs.getString("role")
                );

            }
        }
        return null;
    }
    // Get all users (for admin only)
    public static List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, first_name, last_name, email, username, password, is_verified, role FROM users";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getBoolean("is_verified"),
                    rs.getString("role")
                ));
            }
        }
        return users;
    }
 
    // ────────────────────────────────────────────────────────────────────
    // OTP methods
    // ────────────────────────────────────────────────────────────────────
    
    public static void saveOtp(String email, String otp, LocalDateTime expiresAt) throws SQLException {
        String sql = "UPDATE users SET otp_code=?, otp_expires=? WHERE email=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, otp);
            ps.setTimestamp(2, Timestamp.valueOf(expiresAt));
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }
 
    /**
     * Looks up the user by email, checks if the OTP matches and has not expired.
     * If valid, sets is_verified=TRUE and clears the OTP columns.
     *
     * return true if verified successfully, false if code is wrong or expired
     */
    public static boolean verifyOtp(String email, String otp) throws SQLException {
        String sql = "SELECT otp_code, otp_expires FROM users WHERE email=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedOtp     = rs.getString("otp_code");
                Timestamp expiresTs  = rs.getTimestamp("otp_expires");
 
                // Check code matches and has not expired
                if (storedOtp != null && storedOtp.equals(otp)
                        && expiresTs != null
                        && expiresTs.toLocalDateTime().isAfter(LocalDateTime.now())) {
 
                    // Mark verified and clear the OTP so it can't be reused
                    String update = "UPDATE users SET is_verified=TRUE, otp_code=NULL, otp_expires=NULL WHERE email=?";
                    PreparedStatement updatePs = c.prepareStatement(update);
                    updatePs.setString(1, email);
                    updatePs.executeUpdate();
                    updatePs.close();
                    return true;
                }
            }
        }
        return false;
    }
 
    /**
     * Fetches a user by email (used by verify-otp and resend-otp endpoints).
     */
    public static User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, first_name, last_name, email, username, password, is_verified, role FROM users WHERE email=?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getBoolean("is_verified"),
                    rs.getString("role")
                );
            }
        }
        return null;
    }
}