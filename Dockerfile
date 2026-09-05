# =============================================================================
# Stage 1: Build & Package (Multi-Module Maven Build)
# =============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy root POM and module POMs to leverage Docker layer caching
COPY pom.xml ./
COPY commercedna-core/pom.xml ./commercedna-core/
COPY commercedna-identity/pom.xml ./commercedna-identity/
COPY commercedna-catalog/pom.xml ./commercedna-catalog/
COPY commercedna-negotiation/pom.xml ./commercedna-negotiation/
COPY commercedna-settlement/pom.xml ./commercedna-settlement/
COPY commercedna-audit/pom.xml ./commercedna-audit/
COPY commercedna-api/pom.xml ./commercedna-api/

# Download dependencies in offline mode
RUN mvn dependency:go-offline -B || true

# Copy all source files
COPY commercedna-core/src ./commercedna-core/src
COPY commercedna-identity/src ./commercedna-identity/src
COPY commercedna-catalog/src ./commercedna-catalog/src
COPY commercedna-negotiation/src ./commercedna-negotiation/src
COPY commercedna-settlement/src ./commercedna-settlement/src
COPY commercedna-audit/src ./commercedna-audit/src
COPY commercedna-api/src ./commercedna-api/src

# Package API executable jar without running surefire tests in build stage
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Hardened, Unprivileged Runtime Container
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root system group and user (UID/GID 10001)
RUN addgroup -g 10001 appgroup && \
    adduser -u 10001 -G appgroup -s /bin/sh -D appuser && \
    mkdir -p /app && \
    chown -R appuser:appgroup /app

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /build/commercedna-api/target/commercedna-api-*.jar /app/commercedna.jar

# Run as non-root user
USER appuser

# Expose Spring Boot default port
EXPOSE 8080

# Configure JVM flags optimized for container memory constraints
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Container healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -q -O - http://localhost:8080/actuator/health 2>/dev/null || wget -q -O - http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/commercedna.jar"]
