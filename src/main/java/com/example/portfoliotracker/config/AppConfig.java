package com.example.portfoliotracker.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class AppConfig {
    // RestTemplate to download AMFI NAV file
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}