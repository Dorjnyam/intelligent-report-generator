# =========================
# Stage 1: Build with Maven
# =========================
FROM eclipse-temurin:17-jdk-alpine as builder

WORKDIR /app

# Install Maven
RUN apk update && apk add --no-cache maven

# Copy the project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ================================
# Stage 2: Runtime with JDK only
# ================================
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Optional: Install packages (like curl for healthcheck or LaTeX support if needed)
RUN apk update && apk add --no-cache curl && rm -rf /var/cache/apk/*

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create a non-root user
RUN addgroup -S appuser && adduser -S appuser -G appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose Spring Boot default port
EXPOSE 8080

# Health check (Spring Boot actuator endpoint)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start the app
ENTRYPOINT ["java", "-jar", "app.jar"]
