FROM  openjdk:8-jre-slim
WORKDIR /app
COPY target/sso-service-1.0.0.jar .
CMD ["java", "-jar", "-Ddatabase_engine=POSTGRES", "-Dcontext_name=mypostgres", "-Dfile.encoding=utf-8", "sso-service-1.0.0.jar"]
