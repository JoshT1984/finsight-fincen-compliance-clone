# syntax=docker/dockerfile:1.6

########################
# Build stage
########################
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first (faster rebuilds)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B dependency:go-offline

# Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

########################
# Runtime stage
########################
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as non-root (safer for prod)
RUN useradd -r -u 10001 appuser
USER appuser

ENV JAVA_OPTS=""

# Copy the jar produced by Maven
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
