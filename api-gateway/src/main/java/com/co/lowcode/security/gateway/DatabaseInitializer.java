package com.co.lowcode.security.gateway;

import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // Verificar si la base de datos ya está poblada
        if (isDatabaseEmpty()) {
            // Cargar el contenido del volcado SQL
        	 // Cargar el contenido del volcado SQL desde el classpath
            ClassPathResource resource = new ClassPathResource("data.sql");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            String sqlContent = reader.lines().collect(Collectors.joining("\n"));
            reader.close();

            // Ejecutar el contenido SQL para restaurar la base de datos
            entityManager.createNativeQuery(sqlContent).executeUpdate();
        }
    }

    private boolean isDatabaseEmpty() {
        // Realizar una consulta para verificar si hay algún dato en la base de datos
        List<?> result = entityManager.createNativeQuery("SELECT 1 FROM app LIMIT 1").getResultList();
        return result.isEmpty();
    }

    private String loadSqlContent(String location) throws IOException {
        File file = ResourceUtils.getFile(location);
        return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
    }
}