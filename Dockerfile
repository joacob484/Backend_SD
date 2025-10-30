# ======================================
# Build Stage - Maven compilation
# ======================================
FROM maven:3.9.10-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first for better caching
COPY pom.xml .

# Download dependencies (cached layer if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application with proper encoding
RUN mvn clean package -DskipTests -B \
    -Dproject.build.sourceEncoding=UTF-8 \
    -Dmaven.compiler.release=21

# Verify the JAR was created
RUN ls -lh target/*.jar

# ======================================
# Runtime Stage - JRE
# ======================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy JAR from build stage
COPY --from=builder /build/target/*.jar app.jar

# Copy wait-for-services script
COPY wait-for-services.sh /app/wait-for-services.sh
RUN chmod +x /app/wait-for-services.sh

# Install necessary tools
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       postgresql-client \
       netcat-openbsd \
       curl \
    && rm -rf /var/lib/apt/lists/*

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Entry point: wait for services and start backend
ENTRYPOINT ["./wait-for-services.sh", "java", "-jar", "app.jar"]