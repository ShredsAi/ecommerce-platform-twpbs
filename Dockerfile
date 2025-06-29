# Multi-stage build for Inventory Tracking Shred

# Build stage
FROM maven:3.8.4-openjdk-17 as builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src/ ./src/
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-alpine
WORKDIR /app

# Copy the built artifact
COPY --from=builder /app/target/inventory-tracking-shred-*.jar app.jar

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Expose the application port
EXPOSE 8080

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
