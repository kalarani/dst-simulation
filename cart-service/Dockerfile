FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the pre-built JAR from local filesystem into the container
COPY build/libs/cart-service-0.0.1-SNAPSHOT.jar app.jar

# Expose application port (optional)
EXPOSE 8080

# Run the Spring Boot service
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
