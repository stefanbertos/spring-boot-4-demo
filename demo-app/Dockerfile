# Multi-stage build for Demo Application
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace/app

# Copy Maven files
COPY pom.xml .
COPY src src

# Copy the built JAR (assuming it's already built)
# If building from scratch, uncomment the following and remove the next COPY
# RUN mvn clean package -DskipTests
COPY target/*.jar app.jar

# Extract layers
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy extracted layers
COPY --from=build /workspace/app/extracted/dependencies/ ./
COPY --from=build /workspace/app/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/app/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/app/extracted/application/ ./

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
