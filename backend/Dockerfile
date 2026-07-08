# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies to cache them
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install Tesseract OCR and English language data
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-eng libtesseract-dev && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Ensure Tesseract data path is properly configured
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata

COPY --from=build /app/target/mediinsight-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# Create a startup script to handle the database connection string conversion for Render
RUN echo '#!/bin/bash' > /app/start.sh && \
    echo 'if [ -n "$DB_HOST" ]; then' >> /app/start.sh && \
    echo '  export JDBC_URL="jdbc:postgresql://$DB_HOST/$DB_NAME"' >> /app/start.sh && \
    echo '  echo "Starting with PostgreSQL configuration"' >> /app/start.sh && \
    echo '  exec java -jar app.jar --spring.datasource.url="$JDBC_URL" --spring.datasource.driver-class-name=org.postgresql.Driver --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect' >> /app/start.sh && \
    echo 'else' >> /app/start.sh && \
    echo '  echo "Starting with default configuration"' >> /app/start.sh && \
    echo '  exec java -jar app.jar' >> /app/start.sh && \
    echo 'fi' >> /app/start.sh && \
    chmod +x /app/start.sh

ENTRYPOINT ["/app/start.sh"]
