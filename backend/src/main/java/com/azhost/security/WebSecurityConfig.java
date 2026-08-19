package com.azhost.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Health and info endpoints
                .requestMatchers("/api/health", "/api/info").permitAll()
                // Project and deployment endpoints
                .requestMatchers("/api/projects/**", "/api/deployments/**").permitAll()
                // GitHub legacy paths (frontend compatibility)
                .requestMatchers("/api/github/**").permitAll()
                // GitHub integrations paths (Phase 7 spec)
                .requestMatchers("/api/integrations/github/**").permitAll()
                // GitHub webhook receiver (signature-verified, not session-authenticated)
                .requestMatchers("/api/webhooks/github").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
