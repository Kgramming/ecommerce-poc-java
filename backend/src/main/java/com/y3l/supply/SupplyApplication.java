package com.y3l.supply;

import com.y3l.supply.service.SyncService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * E-commerce POC backend (Java/Spring Boot) — stage 2 of the polyglot build.
 * The API surface is frozen in CONTRACT.md and must stay identical across ports.
 */
@SpringBootApplication
public class SupplyApplication implements WebMvcConfigurer {

    private final SyncService sync;

    public SupplyApplication(SyncService sync) {
        this.sync = sync;
    }

    public static void main(String[] args) {
        SpringApplication.run(SupplyApplication.class, args);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:5174")
                .allowCredentials(true)
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onStartup() {
        // never fail boot because ES is napping
        sync.ensureIndex();
    }
}
