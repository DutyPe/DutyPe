# Multi-stage Dockerfile: build with Maven then run the resulting Spring Boot fat JAR

# Build stage
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app
# Copy only what is necessary to take advantage of Docker layer caching
COPY pom.xml .
COPY src ./src
# Run maven package (skip tests by default to speed up CI builds)
RUN mvn -B -DskipTests package

# Run stage
FROM eclipse-temurin:17-jre
ARG JAR_FILE=target/backend-0.0.1-SNAPSHOT.jar
COPY --from=build /app/${JAR_FILE} /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

