const { exec } = require('child_process');
const net = require('net');

// Función para generar un puerto aleatorio entre min y max
const generarPuertoAleatorio = (min, max) => {
  return Math.floor(Math.random() * (max - min + 1)) + min;
};

// Función para verificar si un puerto está en uso
const puertoEnUso = async (puerto) => {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        // El puerto está en uso
        resolve(true);
      } else {
        // Otro error
        reject(err);
      }
    });
    server.once('listening', () => {
      // El puerto no está en uso
      server.close();
      resolve(false);
    });
    server.listen(puerto);
  });
};

// Función para encontrar un puerto disponible
const encontrarPuertoDisponible = async (min, max) => {
  let puerto = generarPuertoAleatorio(min, max);
  while (await puertoEnUso(puerto)) {
    puerto = generarPuertoAleatorio(min, max);
  }
  return puerto;
};

// Ejecuta docker-compose up -d
const ejecutarDockerCompose = () => {
  exec('docker-compose -p app-5 up -d', (error, stdout, stderr) => {
    if (error) {
      console.error(`Ocurrió un error al ejecutar docker-compose: ${error}`);
      return;
    }
    console.log('docker-compose se ejecutó correctamente.');
    console.log(stdout);
  });
};

// Encontrar un puerto disponible entre 1024 y 65535
encontrarPuertoDisponible(1024, 65535)
  .then((puerto) => {
    console.log(`Puerto disponible encontrado: ${puerto}`);
    // Establecer la variable de entorno PUERTO_API con el puerto disponible
    process.env.PUERTO_ALEATORIO = puerto.toString();
    // Ejecutar docker-compose
    ejecutarDockerCompose();
  })
  .catch((err) => {
    console.error('Error al encontrar el puerto disponible:', err);
  });

