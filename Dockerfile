# Build stage — plain JDK + the repo's own Gradle wrapper, so the build always
# uses the same Gradle version as local/CI (a pinned gradle:X image silently
# drifted behind the wrapper and broke the deploy when AGP 9 landed).
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Dependency layer: copy only the build configuration first so Docker can cache
# resolved dependencies across deploys. This layer is only invalidated when the
# build files themselves change — source-only commits skip the re-download.
# All module build files are needed because settings.gradle.kts includes them.
COPY gradlew ./
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY server/build.gradle.kts server/
COPY shared/build.gradle.kts shared/
COPY composeApp/build.gradle.kts composeApp/
# Warm the dependency cache. Tolerates failure (|| true): worst case the layer
# is a no-op and dependencies resolve in the build step below, as before.
RUN ./gradlew :server:dependencies --no-daemon -q > /dev/null 2>&1 || true

# Source layer: copy the rest and build the fat jar
COPY . .
RUN ./gradlew :server:shadowJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/server/build/libs/server-all.jar server.jar
# Informational only — the app binds to the PORT env var (Render injects 10000).
EXPOSE 10000
CMD ["java", "-jar", "server.jar"]
