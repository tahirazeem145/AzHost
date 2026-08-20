package com.azhost.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    private final Environment environment;

    public CorsConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        
        String allowedOriginsProp = environment.getProperty("azhost.security.cors.allowed-origins");
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());

        if (allowedOriginsProp != null && !allowedOriginsProp.trim().isEmpty()) {
            List<String> origins = Arrays.stream(allowedOriginsProp.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!origins.isEmpty()) {
                config.setAllowedOrigins(origins);
            } else {
                setDefaultLocalOrigins(config, activeProfiles);
            }
        } else {
            setDefaultLocalOrigins(config, activeProfiles);
        }

        config.setAllowedHeaders(List.of("Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With", "X-Request-ID"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }

    private void setDefaultLocalOrigins(CorsConfiguration config, List<String> activeProfiles) {
        if (activeProfiles.contains("prod")) {
            throw new IllegalStateException("CORS allowed origins must be explicitly configured in the production profile.");
        }
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
    }
}
