# Multi-stage Docker build for MQ Performance Test
# Stage 1: Extract layers from the JAR
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY target/performance-test-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# Stage 2: Create the runtime image
FROM eclipse-temurin:25-jre-alpine

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy the extracted layers from builder stage
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Expose the web server port for metrics
EXPOSE 8080

# Health check via actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
