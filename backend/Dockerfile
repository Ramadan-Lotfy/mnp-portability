# ---------- Stage 1: build ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Resolve dependencies first: this layer is reused until the pom changes.
# The cache mount keeps ~/.m2 (including the Maven distribution the wrapper downloads) between builds.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Then compile. Tests run via `./mvnw verify` (they need Docker themselves), not inside the image build.
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# curl is only here for the container healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --no-create-home app

COPY --from=build /workspace/target/*.jar app.jar

USER app
EXPOSE 8080

# Size the heap from the container's memory limit instead of the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]