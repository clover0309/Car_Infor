# Multi-stage build for Spring Boot application
FROM openjdk:17-jdk-slim AS builder

# Install necessary tools
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy gradle wrapper and build files
COPY vehicle-tracker-backend/gradle/ gradle/
COPY vehicle-tracker-backend/gradlew .
COPY vehicle-tracker-backend/gradlew.bat .
COPY vehicle-tracker-backend/build.gradle.kts .
COPY vehicle-tracker-backend/settings.gradle.kts .

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY vehicle-tracker-backend/src/ src/

# Build the application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jdk-slim

# Create app user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
