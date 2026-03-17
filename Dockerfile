# Build stage
FROM gradle:8.11-jdk11 AS builder
WORKDIR /app
COPY . .
RUN gradle :server:shadowJar --no-daemon

# Run stage
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=builder /app/server/build/libs/server-all.jar server.jar
EXPOSE 8080
CMD ["java", "-jar", "server.jar"]
