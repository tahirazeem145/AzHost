package com.azhost.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final TenantAuthenticationFilter tenantAuthenticationFilter;

    public WebSecurityConfig(TenantAuthenticationFilter tenantAuthenticationFilter) {
        this.tenantAuthenticationFilter = tenantAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'; object-src 'none';"))
                .frameOptions(frame -> frame.deny())
                .xssProtection(Customizer.withDefaults())
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(tenantAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(Customizer.withDefaults())
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword == null) return false;
                return rawPassword.toString().equals(encodedPassword) || 
                       new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
            }
        };
    }
}
