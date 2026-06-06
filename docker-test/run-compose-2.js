const express = require('express');
const bodyParser = require('body-parser');
const { exec } = require('child_process');
const fs = require('fs');

const yaml = require('js-yaml');

const app = express();
app.use(bodyParser.json());

app.post('/generate-compose', (req, res) => {
  const { domain, path, uuid, fullname, username } = req.body;

  const composeContent = `
version: '3.8'

services:
  postgres:
    image: postgres:latest
    restart: always
    ports:
      - "5431:5432"
    environment:
      POSTGRES_PASSWORD: qwerty
      POSTGRES_USER: sso
      POSTGRES_DB: sso
    networks:
      - network_${uuid}
  redis:
    image: redis
    restart: always
    networks:
      - network_${uuid}
  eureka:
    image: eureka
    restart: always
    networks:
      - network_${uuid}
  api:
    image: api
    restart: always
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.${uuid}.rule=Host(\`${domain}\`) && PathPrefix(\`${path}\`)"
      - "traefik.http.routers.${uuid}.middlewares=${uuid}"
      - "traefik.http.middlewares.${uuid}.stripPrefix.prefixes=${path}"
    command: ["java","-jar", "-Ddatabase_engine=POSTGRES", "-Dcontext_name=mypostgres", "-Dfile.encoding=utf-8", "/app/api-gateway-1.0.0.jar"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9080/admin/health/"]
      interval: 10s  # Intervalo entre chequeos de salud
      timeout: 5s  # Tiempo máximo para esperar una respuesta
      retries: 3 
    networks:
      - network_${uuid}
  auth:
    image: auth
    restart: always
    command: ["java","-jar", "-Ddatabase_engine=POSTGRES", "-Dcontext_name=mypostgres", "-Dfile.encoding=utf-8", "/app/auth-center-1.0.0.jar"]
    networks:
      - network_${uuid}
  sso:
    image: sso
    depends_on:
      api:
        condition: service_healthy
    environment:
      username: ${username}
      fullname: ${fullname}
      domain: ${domain}
      appName: ApiBee
    restart: always
    command: ["java","-jar", "-Ddatabase_engine=POSTGRES", "-Dcontext_name=mypostgres", "-Dfile.encoding=utf-8", "/app/sso-service-1.0.0.jar"]
    networks:
      - network_${uuid}


networks:
  network_${uuid}:
    external: true`;

  fs.writeFile('docker-compose.yml', composeContent, (err) => {
    if (err) {
      console.error('Error al escribir el archivo docker-compose.yml:', err);
      res.status(500).send('Error al generar el archivo docker-compose.yml');
    } else {
      console.log('Archivo docker-compose.yml generado correctamente');
      // Ejecutar docker-compose up -d con el nombre del proyecto dinámico
     

      exec(`docker network create network_${uuid}`, (error, stdout, stderr) => {
        if (error) {
          console.error(`Error al ejecutar docker-compose: ${error}`);
          res.status(500).send(`Error al ejecutar docker-compose: ${error}`);
          return;
        }
        console.log(`Salida de docker-compose: ${stdout}`);
      //  res.status(200).send(`Archivo docker-compose.yml generado y servicios iniciados correctamente`);

        exec(`docker network connect network_${uuid} traefik-traefik-1`, (error, stdout, stderr) => {
            if (error) {
              console.error(`Error al ejecutar docker-compose: ${error}`);
              res.status(500).send(`Error al ejecutar docker-compose: ${error}`);
              return;
            }
            console.log(`Salida de docker-compose: ${stdout}`);
            exec(`docker-compose -p app_${uuid} up -d`, (error, stdout, stderr) => {
                if (error) {
                  console.error(`Error al ejecutar docker-compose: ${error}`);
                  res.status(500).send(`Error al ejecutar docker-compose: ${error}`);
                  return;
                }
                console.log(`Salida de docker-compose: ${stdout}`);
                res.status(200).send(`Archivo docker-compose.yml generado y servicios iniciados correctamente`);
              });
          });
      });


     

      
    }
  });
});

// Ruta POST para recibir parámetros JSON y configurar los servicios
app.post('/configurar', async (req, res) => {
    const params = req.body;

    try {
        // Generar el archivo docker-compose.yml dinámicamente
        generarComposeFile(params);

        // Iniciar los servicios
        exec(`docker-compose -p dq_${params.uuid} up -d`, (error, stdout, stderr) => {
            if (error) {
              console.error(`Error al ejecutar docker-compose: ${error}`);
              res.status(500).send(`Error al ejecutar docker-compose: ${error}`);
              return;
            }
            console.log(`Salida de docker-compose: ${stdout}`);
            res.status(200).send(`Archivo docker-compose.yml generado y servicios iniciados correctamente`);
          });
      
    } catch (error) {
        console.error('Error al configurar e iniciar los servicios:', error);
        res.status(500).send('Error al configurar e iniciar los servicios.');
    }
});

// Función para iniciar los servicios usando Docker Compose
async function iniciarServicios(uuid) {
    
}

// Función para generar el archivo docker-compose.yml
function generarComposeFile(params) {
    var {  port, dbHost, dbPort, dbName, dbUser, dbPassword, newUser, network } = params;

    var dbHost = dbHost || 'postgres';
    var dbPort = dbPort || '5432';
    var dbPassword = dbPassword || 'qwerty';
    var dbUser = dbUser || 'sso';

    const composeConfig = {
        version: '3.8',
        services: {
            [dbName]: {
                image: 'dynamic_query',
               // ports: [`${port}:${port}`],
                environment: {
                    'APP_NAME': dbName,  // Nombre de la aplicación
                    'DB_DRIVER_NAME': 'org.postgresql.Driver',  // Clase del controlador JDBC
                    'DB_HOST': dbHost,
                    'DB_PORT': dbPort,
                    'DB_DATABASE': dbName,
                    'DB_USERNAME': dbUser,
                    'DB_PASSWORD': dbPassword,
                    'PORT': port
                },
                networks: [network]
            }
        },
        networks: {
            [network]: {'external': true}
        }
    };

    const yamlString = yaml.dump(composeConfig);
    fs.writeFileSync('docker-compose.yml', yamlString);
}

app.listen(3000, 'tu-dominio.com', () => {
  console.log('Servidor en ejecución en el puerto 3000');
});