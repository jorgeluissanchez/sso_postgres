const express = require('express');
const fs = require('fs');
const Docker = require('dockerode');
const docker = new Docker();
const yaml = require('js-yaml');
const compose = require('docker-compose');

const app = express();
app.use(express.json()); // Para manejar solicitudes con cuerpo JSON

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
                ports: [`${port}:${port}`],
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

// Función para iniciar los servicios usando Docker Compose
async function iniciarServicios() {
    try {
        await compose.upAll({ cwd: '.', log: true });
        console.log('Servicios iniciados con éxito.');
    } catch (error) {
        console.error('Error al iniciar los servicios:', error);
    }
}

// Ruta POST para recibir parámetros JSON y configurar los servicios
app.post('/configurar', async (req, res) => {
    const params = req.body;

    try {
        // Generar el archivo docker-compose.yml dinámicamente
        generarComposeFile(params);

        // Iniciar los servicios
        await iniciarServicios();

        res.status(200).send('Servicios configurados e iniciados con éxito.');
    } catch (error) {
        console.error('Error al configurar e iniciar los servicios:', error);
        res.status(500).send('Error al configurar e iniciar los servicios.');
    }
});

// Iniciar el servidor en el puerto 3000
const PORT = 3001;
app.listen(PORT, () => {
    console.log(`Servidor escuchando en el puerto ${PORT}`);
});