# 🐛 Smart Bug Tracker

**AI-Assisted Bug Triage & Issue Management System**

A full-stack bug tracking application with **JWT authentication**, **role-based access control (RBAC)**, and **AI-powered bug triage** using Google Gemini. Built with Spring Boot, MySQL, and vanilla JavaScript.

---

## 🔑 Authentication — JWT Login Flow

The application uses **stateless JWT-based authentication**. No sessions are stored on the server.

### Login Flow

```
User opens app → Login page shown → Enters username/password
   → POST /api/auth/login → Backend verifies with BCrypt
   → JWT generated (contains userId, username, role)
   → JWT stored in browser localStorage
   → User redirected to role-specific dashboard
```

### Key Details

| Feature | Implementation |
|---------|---------------|
| Password hashing | BCrypt via `PasswordEncoder` |
| Token format | JWT (HMAC-SHA256 signed) |
| Token expiry | 24 hours |
| Token storage | `localStorage` (frontend) |
| User identification | From `SecurityContext` (not UI selection) |
| Logout | Clears JWT from `localStorage` |
| Session expired | Auto-redirect to login page on 401 |

### Default Credentials

All seeded users share the password: **`password123`**

| Username | Role | Expertise |
|----------|------|-----------|
| `admin` | ADMIN | — |
| `rahul` | DEVELOPER | BACKEND |
| `priya` | DEVELOPER | FRONTEND |
| `arjun` | DEVELOPER | DATABASE |
| `neha` | DEVELOPER | FULL_STACK |
| `tester1` | TESTER | — |
| `tester2` | TESTER | — |
| `tester3` | TESTER | — |

---

## 🛡️ Role-Based Access Control (RBAC)

### ADMIN

- ✅ View **all** issues
- ✅ View AI triage suggestions
- ✅ Accept, modify, or ignore AI suggestions
- ✅ Assign issues to developers
- ✅ View dashboard analytics
- ✅ Create, edit, delete any issue
- ✅ View full duplicate details (Bug ID + similarity)

### DEVELOPER

- ✅ View only **assigned** issues
- ✅ Update issue status (on assigned issues)
- ✅ Add comments
- ❌ Cannot create issues
- ❌ Cannot assign issues
- ❌ Cannot accept/reject AI triage
- ❌ Cannot delete issues

### TESTER

- ✅ Create issues (triggers AI triage automatically)
- ✅ View only **self-reported** issues
- ✅ Verify resolved issues (RESOLVED → CLOSED)
- ✅ Reopen issues (RESOLVED → REOPENED)
- ✅ Add comments
- ⚠️ Can see duplicate warning + similarity %, but **NOT** the duplicate bug ID
- ❌ Cannot edit issues
- ❌ Cannot assign issues
- ❌ Cannot view other testers' issues

### API-Level Security

```
If TESTER calls /api/issues/{id}/assign → 403 Forbidden
If DEVELOPER calls /api/issues/{id}/accept-triage → 403 Forbidden
If token expired → 401 Unauthorized → Auto-redirect to login
```

---

## 🤖 AI-Assisted Bug Triage Workflow

### Step 1: Tester Creates Issue

```
Tester logs in → Navigates to "New Issue" → Fills form:
  - Title
  - Description
  - Steps to Reproduce
  - Expected Result
  - Actual Result
→ Clicks "Create & Analyze with AI"
```

### Step 2: Backend AI Analysis

```
Backend receives issue → Fetches last 50 issues from DB
→ Sends to AI (Gemini API):
   - New issue: title, description, steps, expected, actual
   - Previous issues: ID, title, description (truncated)
→ AI returns suggestions
→ Suggestions stored on issue (NOT automatically applied)
```

**Important:** AI never accesses the database directly. The backend provides the context.

### Step 3: AI Returns

| Field | Example |
|-------|---------|
| Optimized Summary | "Login form crashes with XSS input" |
| Suggested Priority | `CRITICAL` |
| Suggested Expertise | `BACKEND` |
| Possible Duplicate Bug ID | `#3` |
| Duplicate Similarity | `72%` |
| Reason | "Contains crash keywords, XSS vulnerability..." |
| Missing Information | "Missing: Browser version, OS details" |

### Step 4: Triage Status

- `READY_FOR_TRIAGE` — Complete bug report, ready for admin review
- `NEEDS_MORE_INFO` — AI detected missing fields (steps, expected result, etc.)

### Step 5: Fallback (If AI API Fails)

Rule-based keyword matching is used as fallback:

| Keywords | Suggested Priority/Expertise |
|----------|------------------------------|
| crash, data loss, security, vulnerability | CRITICAL |
| payment failed, 500, authentication, broken | HIGH |
| typo, cosmetic, minor, font | LOW |
| api, endpoint, server, controller | BACKEND |
| css, button, layout, page, ui | FRONTEND |
| database, query, sql, hibernate | DATABASE |

Issue creation **never fails** due to AI failure.

---

## 👨‍💼 Admin Issue Review Flow

### Step-by-Step

1. Admin logs in with JWT
2. Navigates to Issues → Clicks on specific issue (e.g., Bug #15)
3. Issue Detail page opens
4. **AI Suggestions panel** is displayed

### AI Suggestions Panel Shows

- 📝 Optimized Summary
- 📊 Suggested Priority (with Accept button)
- 🎯 Suggested Expertise
- 🔍 Possible Duplicate (with similarity % and link to duplicate)
- 💡 AI Reasoning
- ⚠️ Missing Information warnings

### Admin Actions

| Action | Description |
|--------|-------------|
| **Accept AI Priority** | Copies AI-suggested priority to actual priority |
| **View Matching Devs** | Shows developers matching AI-suggested expertise |
| **Assign Developer** | Admin picks final developer from matching list |
| **Ignore Suggestions** | Keep current values, don't apply AI suggestions |

### Developer Matching Logic

```
AI suggests BACKEND expertise →
  Show: Rahul (BACKEND), Neha (FULL_STACK)
  
If no matching developer exists →
  Show: All developers as fallback
```

**AI never auto-assigns developers. Admin is always the final decision maker.**

---

## 🔒 Duplicate Detection Security

| Role | What They See |
|------|---------------|
| TESTER | ⚠️ "Possible Duplicate Found" + similarity % only. Cannot see duplicate Bug ID or open another tester's issue. |
| DEVELOPER | Full duplicate details only for **assigned** issues |
| ADMIN | Full duplicate details for all issues (Bug ID + link) |

---

## 🏗️ Architecture

```
Frontend (Vanilla JS)
  ↕ REST API (JSON + JWT Bearer token)
Backend (Spring Boot)
  ├── Controller → Service → Repository (Clean layered architecture)
  ├── Security: JWT filter → SecurityContext → Role check
  ├── AI Triage: AiTriageService → Gemini API / Rule-based fallback
  └── Database: MySQL (JPA/Hibernate)
```

### Key Files

| Layer | Files |
|-------|-------|
| **Models** | `User.java`, `Issue.java`, `Comment.java` |
| **Enums** | `Role`, `Status`, `Priority`, `Expertise`, `TriageStatus` |
| **Security** | `SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `CustomUserDetails` |
| **Controllers** | `AuthController`, `IssueController`, `CommentController`, `DashboardController`, `UserController` |
| **Services** | `IssueService`, `AiTriageService`, `UserService`, `CommentService` |
| **Repositories** | `IssueRepository`, `UserRepository`, `CommentRepository` |
| **Config** | `SecurityConfig`, `DataInitializer`, `WebConfig` |
| **Frontend** | `index.html`, `app.js`, `api.js`, `style.css` |

---

## 🚀 Running the Application

### Prerequisites

- Java 17+
- MySQL 8.0+
- Maven 3.8+

### Setup

1. Create MySQL database:
   ```sql
   CREATE DATABASE bugtracker;
   ```

2. Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. (Optional) Set Gemini API key for AI triage:
   ```properties
   gemini.api.key=YOUR_API_KEY
   ```
   If not set, rule-based fallback is used automatically.

4. Run:
   ```bash
   ./mvnw spring-boot:run
   ```

5. Open: [http://localhost:8080](http://localhost:8080)

---


## 🧪 Testing

Run tests:
```bash
./mvnw test
```

Tests use H2 in-memory database with separate `test/resources/application.properties`.

---

## 🐳 Docker

```bash
docker-compose up --build
```

See `Dockerfile` and `docker-compose.yml` for container configuration.

---

## 📋 Status Workflow

```
NEW → OPEN → IN_PROGRESS → RESOLVED → CLOSED
                  ↑              ↓
                  ←── REOPENED ←──
```

Valid transitions are enforced by the backend. Invalid transitions return a 400 error.
