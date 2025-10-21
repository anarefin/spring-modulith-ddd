FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and parent pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy application module pom
COPY application/pom.xml application/

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy application source code
COPY application/src application/src

# Build the application
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built jar from application module
COPY --from=build /app/application/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
