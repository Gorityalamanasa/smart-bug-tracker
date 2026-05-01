# ============================================
# Smart Bug Tracker — Multi-stage Docker Build
# ============================================

# Stage 1: Build with Maven
FROM eclipse-temurin:23-jdk AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:23-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Metadata
LABEL maintainer="bugtracker-team"
LABEL description="Smart Bug and Issue Tracking System"
LABEL version="1.0.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
