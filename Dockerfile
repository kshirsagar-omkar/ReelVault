# ============================================
# ReelVault Backend — Dockerfile for Render
# Multi-stage build: Maven → JRE runtime
# ============================================

# Stage 1: Build JAR using Maven + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY backend/pom.xml .
# Download dependencies first (cached layer — only re-downloads when pom.xml changes)
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight runtime with JRE 21 only
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from Stage 1
COPY --from=builder /app/target/reelvault.jar app.jar

# Render uses port 10000 by default
EXPOSE 10000

# JVM tuning for Render free tier (512MB RAM) + force IPv4 for Supabase
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:+UseStringDeduplication -Djava.net.preferIPv4Stack=true"

# Start Spring Boot — PORT env var is set by Render automatically
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar app.jar"]
