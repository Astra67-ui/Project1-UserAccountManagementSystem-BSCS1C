package com.lampara.api;

import com.lampara.db.UserStore;
import com.lampara.model.User;
import com.lampara.util.EmailOtpService;
import com.lampara.util.HttpUtil;
import com.lampara.util.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.security.SecureRandom;

// Authentication Handler Class 
public class AuthHandler implements HttpHandler {

    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (HttpUtil.handleOptions(ex)) return;
 
        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
 
        try {
            if      (path.endsWith("/register")   && "POST".equals(method)) { handleRegister(ex); }
            else if (path.endsWith("/login")       && "POST".equals(method)) { handleLogin(ex); }
            else if (path.endsWith("/logout")      && "POST".equals(method)) { handleLogout(ex); }
            else if (path.endsWith("/verify-otp")  && "POST".equals(method)) { handleVerifyOtp(ex); }
            else if (path.endsWith("/resend-otp")  && "POST".equals(method)) { handleResendOtp(ex); }
            else HttpUtil.sendError(ex, 404, "Not found");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
 
    // ── POST /api/register ───────────────────────────────────────────────
    private void handleRegister(HttpExchange ex) throws Exception {
        JSONObject body = new JSONObject(HttpUtil.readBody(ex));
 
        String firstName = body.optString("firstName").trim();
        String lastName  = body.optString("lastName").trim();
        String email     = body.optString("email").trim();
        String username  = body.optString("username").trim();
        String password  = body.optString("password");
        String confirm   = body.optString("confirmPassword");
 
        // Validate all fields
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || username.isEmpty() || password.isEmpty()) {
            HttpUtil.sendError(ex, 400, "Fill up all  required fields."); return;
        }

        if (!firstName.matches("^[\\p{L} '-]{1,50}$") || !lastName.matches("^[\\p{L} '-]{1,50}$")) {
        HttpUtil.sendError(ex, 400, "Names may only contain letters, spaces, hyphens, and apostrophes.");
        return;
        }
 
        // Email format
        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            HttpUtil.sendError(ex,400, "Email must follow a valid format."); return;
        }
 
        // Password strength
        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!])(?=\\S+$).{8,}$")) {
            HttpUtil.sendError(ex, 400,
                "Password must be 8+ characters with uppercase, lowercase, number, and special character."); return;
        }
 
        // Passwords match
        if (!password.equals(confirm)) {
            HttpUtil.sendError(ex, 400, "Passwords do not match."); return;
        }
 
        // Uniqueness checks
        if (UserStore.usernameExists(username)) {
            HttpUtil.sendError(ex, 409, "Username already taken."); return;
        }
        if (UserStore.emailExists(email)) {
            HttpUtil.sendError(ex, 409, "Email already registered."); return;
        }
 
        // Save the user (is_verified stays FALSE until OTP confirmed)
        User u = new User();
        u.firstName = firstName;
        u.lastName  = lastName;
        u.email     = email;
        u.username  = username;
        u.password  = password;
        UserStore.addUser(u);
 
        // generate OTP, save it, and email it
        String otp = generateOtp();
        UserStore.saveOtp(email, otp, LocalDateTime.now().plusMinutes(10));
 
        try {
            EmailOtpService.sendOtp(email, otp);
            HttpUtil.sendOk(ex, "Registration successful! Please check your email for the verification code.");
        } catch (Exception mailEx) { 
            mailEx.printStackTrace();
            HttpUtil.sendOk(ex, "Account created, but we couldn't send the verification email right now. Use \"Resend code\" on the next screen to try again.");
        }
    }
 
    // ── POST /api/login ──────────────────────────────────────────────────

    private void handleLogin(HttpExchange ex) throws Exception {
        JSONObject body = new JSONObject(HttpUtil.readBody(ex));
        String username = body.optString("username").trim();
        String password = body.optString("password");
 
        if (username.isEmpty() || password.isEmpty()) { // All fields are required
            HttpUtil.sendError(ex, 400, "Enter your username and password."); return;
        }
 
        User user = UserStore.findUser(username, password); // field validation 
        if (user == null) {
            HttpUtil.sendError(ex, 401, "Please enter a valid username and password."); return;
        }
 
        // block unverified accounts
        if (!user.isVerified) {
            HttpUtil.sendError(ex, 403, "Please verify your email before logging in."); return;
        }
 
        String token = SessionStore.createSession(username);
 
        JSONObject resp = new JSONObject()
            .put("success",   true)
            .put("token",     token)
            .put("username",  user.username)
            .put("firstName", user.firstName)
            .put("lastName",  user.lastName)
            .put("email",     user.email)
            .put("role",      user.role);
        HttpUtil.sendJson(ex, 200, resp);
    }
 
    // ── POST /api/logout ─────────────────────────────────────────────────
    private void handleLogout(HttpExchange ex) throws Exception {
        String token = SessionStore.extractToken(ex);
        SessionStore.invalidate(token);
        HttpUtil.sendOk(ex, "Logged out successfully.");
    }
 
    // ── POST /api/verify-otp ─────────────────────────────────────────────

    private void handleVerifyOtp(HttpExchange ex) throws Exception {
        JSONObject body = new JSONObject(HttpUtil.readBody(ex));
        String email = body.optString("email").trim();
        String otp   = body.optString("otp").trim();
 
        if (email.isEmpty() || otp.isEmpty()) {
            HttpUtil.sendError(ex, 400, "Email and OTP are required."); return;
        }
 
        // Check the user exists
        User user = UserStore.getUserByEmail(email);
        if (user == null) {
            HttpUtil.sendError(ex, 404, "No account found with that email."); return;
        }
 
        // Already verified — nothing to do
        if (user.isVerified) {
            HttpUtil.sendOk(ex, "Email is already verified. Please log in."); return;
        }
 
        // Validate OTP (checks match + expiry, then clears it on success)
        boolean verified = UserStore.verifyOtp(email, otp);
        if (!verified) {
            HttpUtil.sendError(ex, 400, "Invalid or expired code. Please try again."); return;
        }
 
        HttpUtil.sendOk(ex, "Email verified successfully! You can now log in.");
    }
 
    // ── POST /api/resend-otp ─────────────────────────────────────────────

    private void handleResendOtp(HttpExchange ex) throws Exception {
        JSONObject body = new JSONObject(HttpUtil.readBody(ex));
        String email = body.optString("email").trim();
 
        if (email.isEmpty()) {
            HttpUtil.sendError(ex, 400, "Email is required."); return;
        }
 
        User user = UserStore.getUserByEmail(email);
        if (user == null) {
            HttpUtil.sendError(ex, 404, "No account found with that email."); return;
        }
 
        if (user.isVerified) {
            HttpUtil.sendError(ex, 400, "Email is already verified."); return;
        }
 
        // Generate new OTP, overwrite the old one, re-send email
        String otp = generateOtp();
        UserStore.saveOtp(email, otp, LocalDateTime.now().plusMinutes(10));
        EmailOtpService.sendOtp(email, otp);
 
        HttpUtil.sendOk(ex, "A new verification code has been sent to your email.");
    }

 
    // generate a random 6-digit OTP 
    private String generateOtp() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }
}
