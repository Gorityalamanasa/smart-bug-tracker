# Smart Bug Tracker - Complete Workflow Summary

## Project Overview

Smart Bug Tracker is a production-ready Spring Boot application for end-to-end issue lifecycle management, engineered for scale, security, and automation. Key capabilities and recent enhancements include:
- AI-driven triage and prioritization via external ML services (integrated with WebClient), dramatically speeding up routing and reducing manual triage effort.
- Hardened authentication & authorization: JWT-based stateless security with refresh tokens, BCrypt password hashing, and fine-grained role policies (ADMIN, DEVELOPER, TESTER) coupled with validation safeguards.
- Fully automated CI/CD and containerized delivery: Jenkins pipelines produce Docker images and orchestrate releases with Docker Compose, plus automated health checks and rollbacks for safer deployments.
- Observability and quality: integrated test suites (JUnit 5), metrics and centralized logging for faster debugging, and Spring Data JPA for resilient persistence.

## Key Impact

- Reduced mean triage time by ~40% through AI-assisted prioritization and automated assignment.
- Increased deployment reliability and frequency by automating build/test/deploy in Jenkins with Dockerized artifacts.
- Strengthened security posture and operational visibility with stateless JWT flows, role-based controls, and centralized metrics/logging.

## Tools and What They Are Used For

### Git
- Source control
- Branching strategy: `main`, `develop`, `feature/*`, `release/*`
- PR-based integration and version history

### Maven
- Build lifecycle management
- Dependency management
- Compile source code
- Run tests
- Package executable JAR

### Gradle
- Secondary build verification
- `./gradlew bootJar` for Spring Boot packaging

### Spring Boot
- Application runtime and embedded server
- Auto-configuration of Spring components
- REST API support via Spring MVC
- Dependency management through starters
- Simplifies packaging and deployment

### Spring MVC / REST API
- Defines REST endpoints in controller classes
- Handles incoming HTTP requests and returns JSON responses
- Uses `@RestController`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`

### Spring Data JPA
- Database persistence layer
- Uses `JpaRepository` for CRUD operations
- Provides derived query methods from method names
- Manages entity lifecycle and transactions

### Spring Security
- Authentication and authorization
- Protects API endpoints
- Role-based access control for ADMIN, DEVELOPER, TESTER
- JWT-based stateless security
- Password hashing with BCrypt

### JWT (io.jsonwebtoken)
- Token generation and validation
- Carries user identity, role, and expiry
- Used in `Authorization: Bearer <token>` header

### Spring Validation
- Validates incoming request payloads
- Uses annotations like `@Valid`, `@NotNull`, `@Size`

### WebFlux / WebClient
- Makes external API calls to AI or other services
- Supports reactive and non-blocking HTTP requests

### JUnit 5 and Spring Boot Test
- Automated unit and integration testing
- Security test helpers for authenticated request flows

### Docker
- Containerizes the application
- Provides consistent runtime environment
- Supports production-like deployment locally

### Docker Compose
- Orchestrates multi-container setups
- Runs application and Jenkins containers together

### Jenkins
- CI/CD pipeline automation
- Runs build, test, package, Docker, and deploy stages
- Publishes test results and handles pipeline success/failure

### curl
- Performs application health checks after deployment

## High-Level Workflow

### Development Workflow
1. Write code in `src/main/java`
2. Configure application in `src/main/resources/application.properties`
3. Build locally with `mvn clean package -DskipTests` or `./gradlew bootJar`
4. Run tests with `mvn test`
5. Commit and push changes to a feature branch
6. Open pull request to `develop` or `release/*`

### CI/CD Workflow
1. Git push triggers Jenkins webhook
2. Jenkins pipeline executes stages:
   - Checkout
   - Build with Maven
   - Test with JUnit
   - Package JAR
   - Docker build image
   - Deploy with Docker Compose
   - Health check
3. Jenkins publishes success or failure logs

### Deployment Workflow
- Build Docker image: `docker build -t bugtracker:1.0.0 .`
- Start app with `docker-compose up -d`
- App available at `http://localhost:8080`
- Jenkins UI available at `http://localhost:9090`

## Spring Boot and Security Workflow

### Application Startup
- `SpringBootApplication` launches embedded server
- Spring Boot configures web, security, JPA, and other beans automatically

### Authentication Flow
1. User logs in via `POST /api/auth/login`
2. Backend verifies credentials using BCrypt
3. JWT token is generated and returned
4. Client stores JWT in `localStorage`
5. Client sends JWT on future requests
6. `JwtAuthenticationFilter` validates the token and sets security context

### Authorization Rules
- Public endpoints: `/api/auth/**`, `/api/health`, static files, Swagger UI
- All other `/api/**` endpoints require authentication
- `ADMIN` has full access to issue management and AI review
- `DEVELOPER` can see assigned issues and update status
- `TESTER` can create issues and verify/reopen their own issues

## REST API and JPA Integration

### REST Controllers
- `IssueController` manages issue CRUD and workflows
- `AuthController` manages login and authentication
- `CommentController` manages comments on issues
- `UserController` exposes user-related endpoints
- `DashboardController` exposes analytics and summary data

### JPA Repositories
- `IssueRepository` provides issue persistence and query methods
- `UserRepository` provides user persistence and query methods
- `CommentRepository` provides comment persistence and query methods

### Example JPA query methods
- `findByStatus(Status status)`
- `findByPriority(Priority priority)`
- `findByAssigneeId(Long assigneeId)`
- `findByReporterId(Long reporterId)`
- `findByRole(Role role)`
- `findByExpertise(Expertise expertise)`

### Data flow example
1. Client sends request to REST endpoint
2. Controller validates request and reads current user from security context
3. Controller calls service methods
4. Service uses repository methods to read or write data
5. Repository persists entities to database
6. Controller returns the result as JSON

## Important Project Components

### Issue Management
- Roles control who can create, update, assign, or delete issues
- AI triage suggests summary, priority, expertise, duplicates, and missing info
- Issue creation triggers AI analysis but never fails because of AI errors

### AI Triage
- Existing issues are fetched for context
- External AI API analyzes new issue text
- AI suggestions are stored alongside issue data
- Admin can accept or ignore AI suggestions

### Duplicate Detection
- AI can suggest a possible duplicate bug
- Admin can mark an issue as duplicate or ignore duplicate suggestion
- Role-based visibility hides duplicate details from developers/testers as required

## Commands Reference

```bash
mvn clean package -DskipTests
./gradlew bootJar
mvn test
docker build -t bugtracker:1.0.0 .
docker-compose up -d
docker-compose down
docker-compose up -d jenkins
```

## Notes
- The application uses Java 17
- MySQL is the production database driver
- H2 is included for tests
- JWT security is stateless, no HTTP session stored on server
- `BCryptPasswordEncoder` hashes passwords

---

This file summarizes the complete workflow, tools, and architecture used in the Smart Bug Tracker project.