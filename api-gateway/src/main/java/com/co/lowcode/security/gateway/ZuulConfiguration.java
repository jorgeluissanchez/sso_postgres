package com.co.lowcode.security.gateway;

import com.netflix.zuul.ZuulFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZuulConfiguration {

    public ZuulConfiguration(){
        //For Spring
    }

    @Bean
    public ZuulFilter buildZuulFilter(){
        return new MyZuulFilter();
    }
    
    @Bean
    public ZuulFilter postZuulFilter(){
        return new PostZuulFilter();
    }
}
