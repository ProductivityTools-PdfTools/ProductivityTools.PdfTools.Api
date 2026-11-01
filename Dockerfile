# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
# Ensure gradlew is executable
RUN chmod +x gradlew
RUN ./gradlew clean build --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre
# LibreOffice (Writer only, without GUI) is used to convert .doc/.docx files to PDF.
# Liberation fonts are metric-compatible with Arial/Times New Roman/Courier New used in most Word documents.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libreoffice-writer-nogui fonts-liberation fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
