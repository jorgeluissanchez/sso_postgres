package com.co.lowcode.security.gateway;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
@EnableZuulProxy
public class APIApplication {

    public static void main(String[] args) {
    	Locale.setDefault(new Locale("es", "CO"));
        SpringApplication.run(APIApplication.class, args);
    }
}
