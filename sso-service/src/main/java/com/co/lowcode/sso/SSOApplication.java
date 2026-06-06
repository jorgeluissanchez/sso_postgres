package com.co.lowcode.sso;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
@EnableDiscoveryClient
@SpringBootApplication
@EnableAsync
public class SSOApplication {

    public static void main(String[] args) {
    	Locale.setDefault(new Locale("es", "CO"));
        SpringApplication.run(SSOApplication.class, args);
    }
}
