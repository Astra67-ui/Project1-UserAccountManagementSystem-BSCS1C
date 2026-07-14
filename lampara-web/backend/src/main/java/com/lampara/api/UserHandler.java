package com.lampara.api;

import com.lampara.db.UserStore;
import com.lampara.model.User;
import com.lampara.util.HttpUtil;
import com.lampara.util.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import java.sql.SQLException;
import java.io.IOException;
import org.json.JSONArray;
import java.util.List;

public class UserHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
    System.out.println("Request: " + ex.getRequestMethod() + " " + ex.getRequestURI().getPath());
    if (HttpUtil.handleOptions(ex)) return;

    String path   = ex.getRequestURI().getPath();
    String method = ex.getRequestMethod();

    // Admin routes - handled separately before auth check
    if (path.contains("/admin/users")) {
        try {
            if ("GET".equals(method))         { handleAdminGetAllUsers(ex); }
            else if ("DELETE".equals(method)) { handleAdminDeleteUser(ex); }
            else HttpUtil.sendError(ex, 405, "Method not allowed");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
        return;
    }

    // Regular user routes - require auth
    String token = SessionStore.extractToken(ex);
    String username = SessionStore.getUsernameForToken(token);
    if (username == null) {
        HttpUtil.sendError(ex, 401, "Unauthorized. Please log in.");
        return;
    }

    try {
        if (path.endsWith("/profile")) {
            if ("GET".equals(method))         handleGetProfile(ex, username);
            else if ("PUT".equals(method))    handleUpdateProfile(ex, username);
            else HttpUtil.sendError(ex, 405, "Method not allowed");
        } else if (path.endsWith("/password") && "PUT".equals(method)) {
            handleChangePassword(ex, username);
        } else if (path.endsWith("/account") && "DELETE".equals(method)) {
            handleDeleteAccount(ex, username, token);
        } else {
            HttpUtil.sendError(ex, 404, "Not found");
        }
    } catch (Exception e) {
        e.printStackTrace();
        HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
    }
}

    // GET /api/user/profile
    private void handleGetProfile(HttpExchange ex, String username) throws Exception {
        User u = UserStore.getUserByUsername(username);
        if (u == null) { HttpUtil.sendError(ex, 404, "User not found."); return; }

        JSONObject resp = new JSONObject()
            .put("success",   true)
            .put("firstName", u.firstName)
            .put("lastName",  u.lastName)
            .put("email",     u.email)
            .put("username",  u.username);
        HttpUtil.sendJson(ex, 200, resp);
    }

    // PUT /api/user/profile
    private void handleUpdateProfile(HttpExchange ex, String username) throws Exception {
        JSONObject body = new JSONObject(HttpUtil.readBody(ex));

        String firstName = body.optString("firstName").trim();
        String lastName  = body.optString("lastName").trim();
        String email     = body.optString("email").trim();
        String password  = body.optString("password"); // confirmation

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            HttpUtil.sendError(ex, 400, "Fill up all fields."); return;
        }
        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            HttpUtil.sendError(ex, 400, "Email must have a proper format."); return;
        }

        // Verify password before saving
        if (!UserStore.verifyPassword(username, password)) {
            HttpUtil.sendError(ex, 403, "Enter a valid password to continue."); return;
        }

        User current = UserStore.getUserByUsername(username);
        if (UserStore.isEmailTakenByOther(email, current.email)) {
            HttpUtil.sendError(ex, 409, "Email already in use by another account."); return;
        }

        UserStore.updateUser(username, firstName, lastName, email);
        HttpUtil.sendOk(ex, "Your account has been updated.");
    }

    // PUT /api/user/password
        private void handleChangePassword(HttpExchange ex, String username) throws Exception {
            JSONObject body = new JSONObject(HttpUtil.readBody(ex));

            String current  = body.optString("currentPassword");
            String newPass  = body.optString("newPassword");
            String confirm  = body.optString("confirmPassword");

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                HttpUtil.sendError(ex, 400, "Fill up all fields."); return;
            }
            if (!UserStore.verifyPassword(username, current)) {
                HttpUtil.sendError(ex, 403, "Current password is incorrect."); return;
            }
            if (!newPass.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!])(?=\\S+$).{8,}$")) {
                HttpUtil.sendError(ex, 400, 
                    "Password must be 8+ characters with uppercase, lowercase, number, and special character."); return;
            }
            if (!newPass.equals(confirm)) {
                HttpUtil.sendError(ex, 400, "Passwords do not match."); return;
            }
            if (newPass.equals(current)) {
                HttpUtil.sendError(ex, 400, "New password must differ from current password."); return;
            }

                UserStore.updatePassword(username, newPass);
                HttpUtil.sendOk(ex, "Your password has been updated.");
            }

            // DELETE /api/user/account
        private void handleDeleteAccount(HttpExchange ex, String username, String token) throws Exception {
            JSONObject body = new JSONObject(HttpUtil.readBody(ex));
            String password = body.optString("password");

            if (!UserStore.verifyPassword(username, password)) {
                HttpUtil.sendError(ex, 403, "Enter a valid password to continue."); return;
            }

            UserStore.removeUser(username);
            SessionStore.invalidate(token);
            HttpUtil.sendOk(ex, "Your account has been deleted.");
        }

        private boolean isAdmin(HttpExchange ex) throws SQLException  {
            String token = SessionStore.extractToken(ex);
            String username = SessionStore.getUsernameForToken(token);
            if (username == null) return false;
            User user = UserStore.getUserByUsername(username);
            return user != null && "admin".equals(user.role);
        }

        private void handleAdminGetAllUsers(HttpExchange ex) throws Exception {
            if (!isAdmin(ex)) {
                HttpUtil.sendError(ex, 403, "Access denied.");
                return;
            }
            List<User> users = UserStore.getAllUsers();
            JSONArray arr = new JSONArray();
            for (User u : users) {
                arr.put(new JSONObject()
                    .put("user_id",   u.user_id)
                    .put("firstName", u.firstName)
                    .put("lastName",  u.lastName)
                    .put("email",     u.email)
                    .put("username",  u.username)
                    .put("isVerified", u.isVerified)
                    .put("role",      u.role));    
            }
            JSONObject resp = new JSONObject();
            resp.put("success", arr);
            HttpUtil.sendJson(ex, 200, resp);
        }

        private void handleAdminDeleteUser(HttpExchange ex) throws Exception {
            if (!isAdmin(ex)) {
                HttpUtil.sendError(ex, 403, "Access denied.");
                return;
            }
            JSONObject body = new JSONObject(HttpUtil.readBody(ex));
            String username = body.optString("username");
            if (username.isEmpty()) {
                HttpUtil.sendError(ex, 400, "Username is required."); return;
            }
            UserStore.removeUser(username);
            HttpUtil.sendOk(ex, "User deleted.");
        }

}