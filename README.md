# 🐛 Smart Bug & Issue Tracking System

A full-stack bug tracking application with CI/CD pipeline integration, built as a DevOps demonstration project.

![Java](https://img.shields.io/badge/Java-23-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-green) ![Maven](https://img.shields.io/badge/Maven-3.9.9-red) ![Docker](https://img.shields.io/badge/Docker-Ready-blue) ![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-yellow)

---

## 📋 Features

- **Issue Management** — Create, update, assign, and track bugs with full lifecycle (NEW → OPEN → IN_PROGRESS → RESOLVED → CLOSED)
- **Priority Tracking** — CRITICAL, HIGH, MEDIUM, LOW priority levels with color-coded badges
- **User Roles** — ADMIN, DEVELOPER, TESTER with role-based access
- **Comments** — Add discussion threads on any issue
- **Dashboard** — Real-time statistics with interactive charts
- **REST API** — Full CRUD endpoints for programmatic access
- **Search & Filter** — Filter issues by status, priority, or full-text search
- **Dark Theme UI** — Modern glassmorphism design with micro-animations

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────┐
│              Frontend (HTML/CSS/JS)           │
│         Served from /static directory         │
├──────────────────────────────────────────────┤
│           REST API Controllers               │
│    IssueController · UserController          │
│  CommentController · DashboardController     │
├──────────────────────────────────────────────┤
│             Service Layer                    │
│   Business logic · Status transitions        │
│   Assignment · Dashboard stats               │
├──────────────────────────────────────────────┤
│          Spring Data JPA Repositories         │
├──────────────────────────────────────────────┤
│           H2 In-Memory Database              │
└──────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 23+
- Maven 3.9+
- Docker (optional, for containerized deployment)

### Run Locally

```bash
# Clone the repository
git clone <repository-url>
cd Devops_Project

# Build and run with Maven
mvn clean package -DskipTests
mvn spring-boot:run

# OR run the JAR directly
java -jar target/smart-bug-tracker-1.0.0.jar
```

Open **http://localhost:8080** in your browser.

### Run with Docker

```bash
# Build and start with Docker Compose
docker-compose up -d bugtracker

# View logs
docker-compose logs -f bugtracker

# Stop
docker-compose down
```

### Run with Gradle

```bash
# Using Gradle wrapper
./gradlew bootRun

# Or build and run JAR
./gradlew bootJar
java -jar build/libs/smart-bug-tracker-1.0.0.jar
```

---

## 📡 API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/issues` | List all issues (supports `?status=`, `?priority=`, `?assigneeId=`) |
| `GET` | `/api/issues/{id}` | Get issue details |
| `POST` | `/api/issues?reporterId={id}` | Create new issue |
| `PUT` | `/api/issues/{id}` | Update issue |
| `PATCH` | `/api/issues/{id}/status` | Change status (`{"status": "OPEN"}`) |
| `PATCH` | `/api/issues/{id}/assign` | Assign issue (`{"assigneeId": 2}`) |
| `DELETE` | `/api/issues/{id}` | Delete issue |
| `GET` | `/api/issues/{id}/comments` | List comments |
| `POST` | `/api/issues/{id}/comments` | Add comment (`{"authorId": 1, "content": "..."}`) |
| `GET` | `/api/users` | List users (supports `?role=DEVELOPER`) |
| `GET` | `/api/dashboard/stats` | Dashboard statistics |
| `GET` | `/api/health` | Health check |

---

## 🔄 CI/CD Pipeline

### Jenkins Pipeline Stages

```
Checkout → Build (Maven) → Test → Package → Build (Gradle) → Docker Build → Deploy → Health Check
```

### Setup Jenkins

```bash
# Start Jenkins with Docker Compose
docker-compose up -d jenkins

# Access Jenkins at http://localhost:9090
# Get initial admin password:
docker-compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Configure a **Pipeline** job pointing to the `Jenkinsfile` in the repository root.

---

## 🗂️ Project Structure

```
Devops_Project/
├── src/main/java/com/bugtracker/
│   ├── BugTrackerApplication.java     # Entry point
│   ├── controller/                    # REST endpoints
│   ├── service/                       # Business logic
│   ├── model/                         # JPA entities & enums
│   ├── repository/                    # Data access
│   └── config/                        # Web config & data seeding
├── src/main/resources/
│   ├── application.properties         # App configuration
│   └── static/                        # Frontend (HTML/CSS/JS)
├── src/test/                          # Unit & integration tests
├── pom.xml                            # Maven build
├── build.gradle                       # Gradle build
├── Dockerfile                         # Multi-stage Docker build
├── docker-compose.yml                 # App + Jenkins orchestration
├── Jenkinsfile                        # CI/CD pipeline definition
└── docs/                              # Documentation
```

---

## 🧪 Testing

```bash
# Run all tests with Maven
mvn test

# Run all tests with Gradle
./gradlew test
```

**13 tests** covering:
- Application context loading
- Issue CRUD operations
- Status transition validation
- Auto-assignment behavior
- Dashboard statistics
- Health check endpoint

---

## 🌿 Git Branching Strategy

```
main ──────────────────── Production-ready code
  └── develop ─────────── Integration branch
        ├── feature/*  ── Feature branches
        └── release/*  ── Release candidates
```

---

## 📝 License

This project is built for educational and DevOps demonstration purposes.
