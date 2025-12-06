# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
# Ensure gradlew is executable
RUN chmod +x gradlew
RUN ./gradlew clean build --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
