package com.azhost.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ProductionConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ProductionConfigValidator.class);
    private final Environment environment;

    public ProductionConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("prod")) {
            logger.info("[AZHOST PRODUCTION VALIDATOR] Production profile is active. Validating mandatory configuration...");
            
            validateProperty("spring.datasource.url", "DATABASE_URL");
            validateProperty("spring.datasource.username", "DATABASE_USERNAME");
            validateProperty("spring.datasource.password", "DATABASE_PASSWORD");
            validateProperty("azhost.security.github-token-encryption-key", "ENCRYPTION_KEY / GITHUB_TOKEN_ENCRYPTION_KEY");
            validateProperty("azhost.github.webhook-secret", "GITHUB_WEBHOOK_SECRET");
            validateProperty("supabase.url", "SUPABASE_URL");
            validateProperty("supabase.service-role-key", "SUPABASE_SERVICE_ROLE_KEY");
            validateProperty("azhost.security.cors.allowed-origins", "CORS_ALLOWED_ORIGINS");

            logger.info("[AZHOST PRODUCTION VALIDATOR] All required configuration variables are present.");
        }
    }

    private void validateProperty(String propertyKey, String envName) {
        String value = environment.getProperty(propertyKey);
        if (value == null || value.trim().isEmpty() || value.contains("dummy-") || value.contains("placeholder")) {
            String errorMsg = String.format("Mandatory production configuration missing: %s is not set (Property: %s)", envName, propertyKey);
            logger.error("[AZHOST PRODUCTION VALIDATOR] " + errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }
}
