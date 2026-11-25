# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
# Ensure gradlew is executable
RUN chmod +x gradlew
RUN ./gradlew clean build --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre
# LibreOffice (Writer, Calc and Impress, without GUI) is used to convert .doc/.docx/.docm, .xls/.xlsx and .pptx files to PDF.
# Liberation, Carlito and Caladea fonts are metric-compatible with Arial/Times New Roman/Courier New/Calibri/Cambria used in Office files.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice-writer-nogui \
        libreoffice-calc-nogui \
        libreoffice-impress-nogui \
        fonts-liberation \
        fonts-crosextra-carlito \
        fonts-crosextra-caladea \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
