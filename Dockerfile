# SPRINT 4 - OPTIMIZED DOCKERFILE FOR PRODUCTION DEPLOYMENT
# Multi-stage build optimisé pour 500+ utilisateurs simultanés

# ==========================================
# STAGE 1: Frontend Build (Angular)
# ==========================================
FROM node:20-alpine AS frontend-builder

LABEL stage=frontend-builder
LABEL description="Fortnite Pronos Frontend Build Stage"

# Set working directory
WORKDIR /app/frontend

# Install dependencies first (for better caching)
COPY frontend/package*.json ./
RUN npm ci --only=production --silent

# Copy source code and build
COPY frontend/ ./
RUN npm run build --prod

# ==========================================
# STAGE 2: Backend Build (Spring Boot)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS backend-builder

LABEL stage=backend-builder
LABEL description="Fortnite Pronos Backend Build Stage"

# Install Maven
RUN apk add --no-cache maven

# Set working directory
WORKDIR /app

# Copy Maven configuration first (for better caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src/ src/
COPY --from=frontend-builder /app/frontend/dist/frontend src/main/resources/static/

# Build the application (skip tests for production build)
RUN mvn clean package -DskipTests -B

# ==========================================
# STAGE 3: Production Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS production

LABEL maintainer="Fortnite Pronos Team"
LABEL description="Fortnite Pronos Application - Production Ready"
LABEL version="1.0.0"

# Create non-root user for security
RUN addgroup -g 1000 fortnite && \
    adduser -D -s /bin/sh -u 1000 -G fortnite fortnite

# Install necessary packages
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# Set timezone
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Create application directory
WORKDIR /app

# Copy the JAR from builder stage
COPY --from=backend-builder /app/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R fortnite:fortnite /app

# Switch to non-root user
USER fortnite

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers (500+ users)
ENV JAVA_OPTS="-server \
    -Xms2g \
    -Xmx4g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:+UseCompressedOops \
    -XX:+UseCompressedClassPointers \
    -XX:+AlwaysPreTouch \
    -XX:+UseTLAB \
    -XX:+ResizeTLAB \
    -XX:+PerfDisableSharedMem \
    -XX:+UseTransparentHugePages \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Europe/Paris"

# Spring Boot production profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV LOGGING_LEVEL_ROOT=INFO

# Application entry point
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]