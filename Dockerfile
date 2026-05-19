# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies to cache them
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build package
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install ca-certificates and openssl to fix TLS/SSL connection issues (e.g. MongoDB Atlas)
RUN apk add --no-cache ca-certificates openssl && update-ca-certificates

# Add a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the default port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
