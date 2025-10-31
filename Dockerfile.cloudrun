# ======================================
# Build Stage - Optimized for Cloud Run
# ======================================
FROM maven:3.9.10-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first (better caching)
COPY pom.xml .

# Download dependencies only once (cached layer)
RUN echo "=== Downloading Maven dependencies ===" && \
    mvn dependency:go-offline -B -U

# Copy source code
COPY src ./src

# Build application with optimizations
RUN echo "=== Building application ===" && \
    mvn clean package -DskipTests -B \
    -Dproject.build.sourceEncoding=UTF-8 \
    -Dmaven.compiler.release=21 && \
    echo "=== Build completed ===" && \
    ls -lh target/*.jar

# Verify JAR content
RUN jar tf target/*.jar | head -20

# ======================================
# Runtime Stage - Minimal JRE for Cloud Run
# ======================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy JAR from build stage
COPY --from=builder /build/target/*.jar app.jar

# Verify JAR
RUN ls -lh app.jar

# Install curl for health checks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE 8080

# Optimized JVM flags for Cloud Run
# - InitialRAMPercentage/MaxRAMPercentage: Auto-size based on container memory
# - UseContainerSupport: Respect container limits
# - Faster startup flags
CMD ["sh", "-c", "exec java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=cloudrun \
    -jar /app/app.jar"]
