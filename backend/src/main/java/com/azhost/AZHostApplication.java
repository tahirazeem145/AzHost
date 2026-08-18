package com.azhost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AZHostApplication {

    private static final Logger logger = LoggerFactory.getLogger(AZHostApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AZHostApplication.class, args);
        logger.info("==========================================");
        logger.info("  AZHost Backend Service Started");
        logger.info("  Phase 1 — Foundation Architecture");
        logger.info("==========================================");
    }
}
