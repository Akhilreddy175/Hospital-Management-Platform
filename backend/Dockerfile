# ==========================================
# Stage 1: Build Application with Maven
# ==========================================
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Cache dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build jar
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Minimal, Secure Runtime Image
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security best practice: Create a non-root group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/hospital-appointment-service-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose internal Spring Boot port
EXPOSE 8080

# Environment variables with defaults
ENV APP_PORT=8080 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Healthcheck leveraging Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
