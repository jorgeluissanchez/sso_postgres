# Etapa de construcción
FROM --platform=linux/arm64 maven:3.3-jdk-8 AS build
WORKDIR /app
# Copia solo el archivo de definición de dependencias
COPY pom.xml .
# Descarga las dependencias de Maven y las guarda en caché
# Copia el código fuente
COPY . .
# Compila la aplicación
RUN mvn -f ldap/pom.xml install 
RUN mvn  package

