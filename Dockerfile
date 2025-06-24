# Use Eclipse Temurin JRE 17 Alpine as base image
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Copy the built JAR file
COPY target/order-creation-shred-1.0.0.jar order-creation-shred.jar

# Change ownership of the JAR file
RUN chown appuser:appgroup order-creation-shred.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar order-creation-shred.jar"]