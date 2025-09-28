# Build stage
FROM maven:3.9.10-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiamos el JAR final
COPY --from=builder /app/target/*.jar app.jar

# Copiamos script de espera
COPY wait-for-services.sh /app/wait-for-services.sh
RUN chmod +x /app/wait-for-services.sh

# Instalar herramientas necesarias
RUN apt-get update \
    && apt-get install -y unzip postgresql-client netcat-openbsd \
    && rm -rf /var/lib/apt/lists/*

EXPOSE 8080

# Entry point: espera servicios y arranca backend
ENTRYPOINT ["./wait-for-services.sh", "java", "-jar", "app.jar"]