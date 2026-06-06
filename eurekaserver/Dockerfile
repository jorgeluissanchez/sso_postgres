FROM  openjdk:8-jre-slim
WORKDIR /app
COPY  target/eurekaserver-0.0.1-SNAPSHOT.jar .
CMD ["java", "-Xmx512m", "-jar", "eurekaserver-0.0.1-SNAPSHOT.jar"]
EXPOSE 9761
