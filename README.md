#  LAMPARA — Web-Based User Account Management System

---
## ⚙️ Setup (Step by Step)

### 1. Database
Open **phpMyAdmin** or MySQL Workbench and run:
```sql
-- contents of backend/schema.sql
CREATE DATABASE IF NOT EXISTS lampara_db ...
```

### 2. Configure credentials
Edit `backend/src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/lampara_db?useSSL=false&serverTimezone=UTC
db.user=root
db.password=          ← your MySQL password (empty for XAMPP default)
```

### 3. Build the backend (requires Java 17+ and Maven)
```bash
cd backend
mvn clean package
```
This produces `backend/target/lampara-backend.jar`

### 4. Run the backend
```bash
java -jar backend/target/lampara-backend.jar
```
You should see:
```
╔════════════════════════════════════════╗
║  LAMPARA Backend running on port 8080  ║
╚════════════════════════════════════════╝
```

### 5. Open the frontend
Simply open `frontend/index.html` in your browser.
> No web server needed for the frontend — it talks directly to `localhost:8080`.

---

## 🔌 REST API Reference

| Method | Endpoint              | Auth? | Description          |
|--------|-----------------------|-------|----------------------|
| POST   | /api/register         | ✗     | Create account       |
| POST   | /api/login            | ✗     | Login, returns token |
| POST   | /api/logout           | ✓     | Invalidate session   |
| GET    | /api/user/profile     | ✓     | Get profile          |
| PUT    | /api/user/profile     | ✓     | Update profile       |
| PUT    | /api/user/password    | ✓     | Change password      |
| DELETE | /api/user/account     | ✓     | Delete account       |

Auth uses `Authorization: Bearer <token>` header.

---

## ✅ Features

- Registration with full validation (email format, password strength, uniqueness)
- BCrypt password hashing
- Session-based auth (in-memory token store)
- Dashboard: view profile, edit profile (with password confirmation), change password, delete account
- Password strength meter
- Toast notifications
- Responsive layout

---

## 🔮 Adding Email OTP (optional next step)

See the previous guide — add `EmailOtpService.java` and a `/api/verify-otp` endpoint,
then call it from the frontend after successful registration.

# Team Members: 
1. Bautista, Lucky P.
2. Ammay, Gabriel F.
3. Miguel, Jeril
