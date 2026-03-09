# ── Stage 1: Build ────────────────────────────────────────────────────────
FROM gradle:8.7-jdk21 AS builder
WORKDIR /app

# Copy dependency files first — Docker caches this layer
# and only re-downloads dependencies when build files change
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon

# Copy source and build
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S logistics && adduser -S logistics -G logistics
USER logistics

COPY --from=builder /app/build/libs/*.jar app.jar

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]