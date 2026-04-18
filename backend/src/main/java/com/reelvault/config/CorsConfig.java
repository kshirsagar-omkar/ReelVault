package com.reelvault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration — allows the Vercel-hosted frontend to call the
 * Railway-hosted Spring Boot backend without browser preflight rejections.
 *
 * <p>The allowed origin is read from the CORS_ALLOWED_ORIGIN environment
 * variable. Set this to your exact Vercel URL in production, e.g.:
 * https://reelvault.vercel.app
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigin)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // SEC-5: Explicit header allowlist instead of wildcard "*"
                        .allowedHeaders("Content-Type", "Accept", "Authorization", "X-Requested-With")
                        .exposedHeaders("Content-Type")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
