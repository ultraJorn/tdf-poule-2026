# Multi-stage build: the "build" stage has its own JDK 21 + Maven, so nothing needs to be
# installed on the host machine at all -- only Docker.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy just the POM first and resolve dependencies, so this layer is cached and re-downloads
# don't happen on every source change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
