# syntax=docker/dockerfile:1.7
#
# Multi-stage Dockerfile for the api-gateway module.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY eurekaserver/pom.xml eurekaserver/
COPY common/pom.xml common/
COPY auth-center/pom.xml auth-center/
COPY api-gateway/pom.xml api-gateway/
COPY hello-service/pom.xml hello-service/

RUN mvn -B -ntp -pl api-gateway -am dependency:go-offline -q

COPY common/src common/src
COPY api-gateway/src api-gateway/src
RUN mvn -B -ntp -pl api-gateway -am clean package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system --gid 1000 app && \
    useradd  --system --uid 1000 --gid app --home /app app

COPY --from=build --chown=app:app /workspace/api-gateway/target/api-gateway.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
