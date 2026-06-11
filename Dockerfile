FROM maven:3.9 AS build

WORKDIR /app

COPY backend/pom.xml .
COPY backend/src ./src

RUN mvn clean package -DskipTests

FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/target/my-new-work-1.0.0.jar app.jar

EXPOSE 18080

ENTRYPOINT ["java", "-jar", "app.jar"]
