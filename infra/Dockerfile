# Build context must be the repo root (see infra/DEPLOY.md for the exact
# Render/Railway settings) because the Maven project lives in Application/.

# --- Build stage ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY Application/.mvn/ .mvn/
COPY Application/mvnw Application/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY Application/src ./src
RUN ./mvnw -B package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# H2 console off by default in the container; override locally if you need it.
ENV H2_CONSOLE_ENABLED=false

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
