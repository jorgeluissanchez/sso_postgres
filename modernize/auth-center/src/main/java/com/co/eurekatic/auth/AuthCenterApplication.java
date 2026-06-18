package com.co.eurekatic.auth;

import com.co.eurekatic.common.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for auth-center. The module is a Spring Boot 3.5 application
 * (servlet), registered with Eureka, that issues JWTs for the SSO.
 *
 * <p>The {@code common} module's entities, repositories, and JWT service
 * live outside this app's default base package, so {@code ComponentScan},
 * {@code EntityScan}, and {@code EnableJpaRepositories} all have to
 * point at the right packages explicitly.
 */
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.co.eurekatic.auth",
        "com.co.eurekatic.common.entity",
        "com.co.eurekatic.common.repository",
        "com.co.eurekatic.common.security"
})
@EntityScan(basePackages = "com.co.eurekatic.common.entity")
@EnableJpaRepositories(basePackages = "com.co.eurekatic.common.repository")
@EnableConfigurationProperties(JwtProperties.class)
public class AuthCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthCenterApplication.class, args);
    }
}
