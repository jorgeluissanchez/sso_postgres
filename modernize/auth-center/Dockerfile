# syntax=docker/dockerfile:1.7
#
# Multi-stage Dockerfile for the auth-center module.
# Same pattern as eurekaserver/Dockerfile — multi-stage, slim JRE
# runtime, non-root user. The jar name is auth-center.jar (matches
# the <finalName>${project.artifactId}</finalName> in pom.xml).

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY eurekaserver/pom.xml eurekaserver/
COPY common/pom.xml common/
COPY auth-center/pom.xml auth-center/
COPY api-gateway/pom.xml api-gateway/
COPY hello-service/pom.xml hello-service/

RUN mvn -B -ntp -pl auth-center -am dependency:go-offline -q

COPY common/src common/src
COPY auth-center/src auth-center/src
RUN mvn -B -ntp -pl auth-center -am clean package -DskipTests -q

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system --gid 1000 app && \
    useradd  --system --uid 1000 --gid app --home /app app

COPY --from=build --chown=app:app /workspace/auth-center/target/auth-center.jar /app/app.jar

USER app
EXPOSE 8081

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
