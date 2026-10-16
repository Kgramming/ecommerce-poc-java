package com.y3l.supply.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.y3l.supply.service.EsClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public EsClient esClient(@Value("${app.es.url}") String esUrl) {
        return new EsClient(esUrl);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
