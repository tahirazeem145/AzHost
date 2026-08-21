package com.azhost.github.security;

import com.azhost.entity.User;
import com.azhost.github.entity.OAuthStateTokenEntity;
import com.azhost.github.repository.OAuthStateTokenRepository;
import com.azhost.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubOAuthStateService {

    private static final long STATE_TTL_SECONDS = 300; // 5 minutes
    private final SecureRandom secureRandom = new SecureRandom();
    
    private final OAuthStateTokenRepository tokenRepository;
    private final UserRepository userRepository;
    
    // In-memory fallback for un-wired unit test instances
    private final Map<String, OAuthStateEntry> fallbackStateStore = new ConcurrentHashMap<>();

    private static class OAuthStateEntry {
        private final UUID userId;
        private final Instant expiresAt;

        public OAuthStateEntry(UUID userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }

        public UUID getUserId() {
            return userId;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public GitHubOAuthStateService() {
        this.tokenRepository = null;
        this.userRepository = null;
    }

    @Autowired
    public GitHubOAuthStateService(OAuthStateTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String generateState(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null when generating OAuth state");
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        if (tokenRepository != null && userRepository != null) {
            tokenRepository.deleteByExpiresAtBefore(ZonedDateTime.now());
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                OAuthStateTokenEntity entity = new OAuthStateTokenEntity(stateToken, user, ZonedDateTime.now().plusSeconds(STATE_TTL_SECONDS));
                tokenRepository.save(entity);
                return stateToken;
            }
        }

        // Fallback to in-memory store
        purgeExpiredStatesFallback();
        fallbackStateStore.put(stateToken, new OAuthStateEntry(userId, Instant.now().plusSeconds(STATE_TTL_SECONDS)));
        return stateToken;
    }

    @Transactional
    public boolean validateAndConsumeState(String stateToken, UUID userId) {
        if (stateToken == null || stateToken.isBlank() || userId == null) {
            return false;
        }

        if (tokenRepository != null) {
            Optional<OAuthStateTokenEntity> optionalToken = tokenRepository.findByStateTokenAndUserId(stateToken, userId);
            if (optionalToken.isPresent()) {
                OAuthStateTokenEntity tokenEntity = optionalToken.get();
                tokenRepository.delete(tokenEntity);
                return tokenEntity.getExpiresAt().isAfter(ZonedDateTime.now());
            }
        }

        OAuthStateEntry entry = fallbackStateStore.remove(stateToken);
        if (entry == null || entry.isExpired()) {
            return false;
        }

        return entry.getUserId().equals(userId);
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void purgeExpiredStates() {
        if (tokenRepository != null) {
            tokenRepository.deleteByExpiresAtBefore(ZonedDateTime.now());
        }
        purgeExpiredStatesFallback();
    }

    private void purgeExpiredStatesFallback() {
        fallbackStateStore.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
