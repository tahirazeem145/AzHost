package com.azhost.github.security;

import com.azhost.entity.User;
import com.azhost.github.entity.OAuthStateTokenEntity;
import com.azhost.github.repository.OAuthStateTokenRepository;
import com.azhost.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class GitHubOAuthStateService {

    private static final long STATE_TTL_SECONDS = 300; // 5 minutes
    private final SecureRandom secureRandom = new SecureRandom();
    private final OAuthStateTokenRepository stateTokenRepository;
    private final UserRepository userRepository;

    public GitHubOAuthStateService(OAuthStateTokenRepository stateTokenRepository, UserRepository userRepository) {
        this.stateTokenRepository = stateTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String generateState(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null when generating OAuth state");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        OAuthStateTokenEntity entity = new OAuthStateTokenEntity(
                stateToken,
                user,
                ZonedDateTime.now().plusSeconds(STATE_TTL_SECONDS)
        );
        stateTokenRepository.save(entity);

        return stateToken;
    }

    @Transactional
    public boolean validateAndConsumeState(String stateToken, UUID userId) {
        if (stateToken == null || stateToken.isBlank() || userId == null) {
            return false;
        }

        Optional<OAuthStateTokenEntity> optionalToken = stateTokenRepository.findByStateTokenAndUserId(stateToken, userId);
        if (optionalToken.isEmpty()) {
            return false;
        }

        OAuthStateTokenEntity tokenEntity = optionalToken.get();
        stateTokenRepository.delete(tokenEntity); // Single-use token: remove immediately

        if (tokenEntity.getExpiresAt().isBefore(ZonedDateTime.now())) {
            return false;
        }

        return true;
    }

    @Scheduled(fixedDelay = 300000) // Purge expired OAuth tokens every 5 minutes
    @Transactional
    public void purgeExpiredStates() {
        stateTokenRepository.deleteByExpiresAtBefore(ZonedDateTime.now());
    }
}
