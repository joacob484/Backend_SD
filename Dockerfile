# Build stage
FROM maven:3.9.10-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiamos el JAR final a /app/app.jar
COPY --from=builder /app/target/*.jar app.jar

# Instalar unzip (opcional, útil para inspeccionar el JAR dentro del contenedor)
RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
