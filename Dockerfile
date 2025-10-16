FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and parent pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy all module poms
COPY product-module/pom.xml product-module/
COPY order-module/pom.xml order-module/
COPY payment-module/pom.xml payment-module/
COPY application/pom.xml application/

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy all module source code
COPY product-module/src product-module/src
COPY order-module/src order-module/src
COPY payment-module/src payment-module/src
COPY application/src application/src

# Build all modules (from parent)
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built jar from application module
COPY --from=build /app/application/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
