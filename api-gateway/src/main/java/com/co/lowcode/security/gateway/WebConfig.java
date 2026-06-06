package com.co.lowcode.security.gateway;

import java.io.UnsupportedEncodingException;

import org.springframework.cloud.netflix.zuul.filters.pre.FormBodyWrapperFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Bean
    FormBodyWrapperFilter formBodyWrapperFilter() {
        return new FormBodyWrapperFilter(new MyFormHttpMessageConverter());
    }

    private class MyFormHttpMessageConverter extends FormHttpMessageConverter {

        private byte[] getAsciiBytes(String name) {
            try {
                // THIS IS THE ONLY MODIFICATION:
                return name.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                // Should not happen - US-ASCII is always supported.
                throw new IllegalStateException(ex);
            }
        }
    }
}