FROM  openjdk:8-jre-slim
WORKDIR /app
RUN apt-get update && apt-get install -y curl
COPY target/api-gateway-1.0.0.jar /app/api-gateway-1.0.0.jar  
EXPOSE 9080
