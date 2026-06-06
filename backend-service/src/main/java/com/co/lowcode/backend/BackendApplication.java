package com.co.lowcode.backend;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
@EnableDiscoveryClient
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
    	Locale.setDefault(new Locale("es", "CO"));
        SpringApplication.run(BackendApplication.class, args);
    }
}
