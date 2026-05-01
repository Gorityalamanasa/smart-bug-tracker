# DevOps Workflow

## CI/CD Pipeline

The Jenkins pipeline automates the full build-test-deploy lifecycle.

### Pipeline Stages

| Stage | Tool | Action |
|-------|------|--------|
| Checkout | Git | Pull latest code |
| Build (Maven) | Maven 3.9 | Compile source code |
| Test | JUnit 5 + MockMvc | Run 13 automated tests |
| Package | Maven | Create executable JAR |
| Build (Gradle) | Gradle 8.x | Verify Gradle build |
| Docker Build | Docker | Create container image |
| Deploy | Docker Compose | Start application container |
| Health Check | curl | Verify app is responsive |

### Pipeline Flow

```
Git Push → Jenkins Webhook → Build → Test → Package → Docker → Deploy → Health Check
   │                                                                        │
   └── On failure: Notify team ◄──────────────────────────────────────────┘
```

## Docker Setup

### Application Container
- **Base image**: eclipse-temurin:23 (multi-stage build)
- **Port**: 8080
- **Health check**: /api/health every 30s

### Jenkins Container
- **Image**: jenkins/jenkins:lts
- **Port**: 9090 (web UI), 50000 (agent)
- **Volume**: jenkins_data for persistence

## Git Branching Strategy

- `main` — Production-ready, merged from release branches
- `develop` — Integration branch for feature merges
- `feature/*` — Individual features, merged to develop via PR
- `release/*` — Release candidates, merged to main after QA

## Commands Reference

```bash
# Build
mvn clean package -DskipTests
./gradlew bootJar

# Test
mvn test
./gradlew test

# Docker
docker build -t bugtracker:1.0.0 .
docker-compose up -d
docker-compose down

# Jenkins
docker-compose up -d jenkins
docker-compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
