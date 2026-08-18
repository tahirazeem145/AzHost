package com.azhost.github.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubOAuthStateService {

    private static final long STATE_TTL_SECONDS = 300; // 5 minutes
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OAuthStateEntry> stateStore = new ConcurrentHashMap<>();

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

    public String generateState(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null when generating OAuth state");
        }

        purgeExpiredStates();

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        OAuthStateEntry entry = new OAuthStateEntry(userId, Instant.now().plusSeconds(STATE_TTL_SECONDS));
        stateStore.put(stateToken, entry);

        return stateToken;
    }

    public boolean validateAndConsumeState(String stateToken, UUID userId) {
        if (stateToken == null || stateToken.isBlank() || userId == null) {
            return false;
        }

        OAuthStateEntry entry = stateStore.remove(stateToken); // Single-use: remove immediately
        if (entry == null) {
            return false; // Missing or already used state
        }

        if (entry.isExpired()) {
            return false; // Expired state
        }

        return entry.getUserId().equals(userId); // Must match authenticated user
    }

    private void purgeExpiredStates() {
        Instant now = Instant.now();
        stateStore.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
