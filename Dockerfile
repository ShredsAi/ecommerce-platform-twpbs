FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the jar file from the build stage
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Environment variables for configuration
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE="prod"

# Expose the application port
EXPOSE 8080

# Health check using wget instead of curl (alpine doesn't come with curl by default)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application with proper memory settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
