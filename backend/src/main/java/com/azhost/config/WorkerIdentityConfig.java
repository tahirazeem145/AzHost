package com.azhost.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.util.UUID;

@Configuration
public class WorkerIdentityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WorkerIdentityConfig.class);

    @Value("${azhost.worker.id:}")
    private String configuredWorkerId;

    @Bean
    public String workerId() {
        if (configuredWorkerId != null && !configuredWorkerId.isBlank()) {
            logger.info("[AZHOST WORKER] Configured Worker ID: {}", configuredWorkerId);
            return configuredWorkerId;
        }

        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String generated = "worker-" + hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
            logger.info("[AZHOST WORKER] Auto-generated Worker ID: {}", generated);
            return generated;
        } catch (Exception e) {
            String fallback = "worker-node-" + UUID.randomUUID().toString().substring(0, 8);
            logger.info("[AZHOST WORKER] Fallback Worker ID: {}", fallback);
            return fallback;
        }
    }
}
