package com.azhost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AZHost Backend Application.
 *
 * @EnableAsync is required for the GitHub webhook processing pipeline:
 * GitHubWebhookService.processPushEventAsync() uses @Async to process
 * webhook events after the 202 response has been sent to GitHub.
 */
@SpringBootApplication
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class AZHostApplication {

    private static final Logger logger = LoggerFactory.getLogger(AZHostApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AZHostApplication.class, args);
        logger.info("==========================================");
        logger.info("  AZHost Backend Service Started");
        logger.info("  Phase 7 — GitHub Source Integration");
        logger.info("==========================================");
    }
}
