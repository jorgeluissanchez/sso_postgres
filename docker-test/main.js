const express = require('express');
const Docker = require('dockerode');
const app = express();

// Crear una instancia de Docker
const docker = new Docker();

// Configuración de Express para parsear JSON en el cuerpo de las solicitudes
app.use(express.json());

// Endpoint POST para ejecutar un contenedor de Docker
app.post('/run-container', async (req, res) => {
    // Obtener los parámetros de la solicitud
    const {  containerName, envVars, networkMode, memoryLimit } = req.body;

    try {
        // Convertir las variables de entorno a un formato adecuado para Docker
        const envArray = Object.entries(envVars).map(([key, value]) => `${key}=${value}`);

        // Configurar las opciones para el contenedor
        const containerOptions = {
            name: containerName,
            Image: '',
            Env: envArray, // Enviar las variables de entorno como una lista de cadenas
            HostConfig: {
                NetworkMode: networkMode || 'bridge', // Modo de red (por defecto, 'bridge')
                Memory: memoryLimit || 700 * 1024 * 1024 // Límite de memoria en bytes (700 MB por defecto)
            }
        };

        // Crear el contenedor
        const container = await docker.createContainer(containerOptions);

        // Iniciar el contenedor
        await container.start();

        // Enviar respuesta exitosa
        res.status(200).json({ message: 'Contenedor iniciado con éxito' });
    } catch (error) {
        // Manejar errores y enviar respuesta de error
        console.error('Error al iniciar el contenedor:', error);
        res.status(500).json({ error: 'Error al iniciar el contenedor' });
    }
});

// Iniciar el servidor en el puerto 3000
app.listen(3001, () => {
    console.log('Servidor escuchando en el puerto 3001');
});