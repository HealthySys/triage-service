FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY patient-service/pom.xml patient-service/
COPY record-service/pom.xml record-service/
COPY triage-service/pom.xml triage-service/
COPY notification-service/pom.xml notification-service/
COPY triage-service/src triage-service/src/
RUN mvn -pl triage-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/triage-service/target/triage-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
