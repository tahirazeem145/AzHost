package com.azhost.config;

import com.azhost.entity.User;
import com.azhost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "test"})
public class DevUserInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevUserInitializer.class);
    public static final String DEV_USER_EMAIL = "developer@azhost.dev";

    private final UserRepository userRepository;

    public DevUserInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(DEV_USER_EMAIL)) {
            User devUser = new User(
                    DEV_USER_EMAIL,
                    "dev_password_hash_placeholder",
                    "Lead Developer"
            );
            userRepository.save(devUser);
            logger.info("Initialized development user context: {}", DEV_USER_EMAIL);
        }
    }
}
