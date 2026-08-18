package com.azhost.github;

import com.azhost.github.security.GitHubOAuthStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GitHubOAuthStateTest {

    private GitHubOAuthStateService stateService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        stateService = new GitHubOAuthStateService();
        userId = UUID.randomUUID();
    }

    @Test
    void shouldGenerateUniqueAndValidStateToken() {
        String stateToken = stateService.generateState(userId);
        assertNotNull(stateToken);
        assertFalse(stateToken.isBlank());

        boolean isValid = stateService.validateAndConsumeState(stateToken, userId);
        assertTrue(isValid, "State token should be valid for the generating user");
    }

    @Test
    void shouldEnforceSingleUseStateConsumption() {
        String stateToken = stateService.generateState(userId);
        assertTrue(stateService.validateAndConsumeState(stateToken, userId));
        assertFalse(stateService.validateAndConsumeState(stateToken, userId), "State token should not be reusable");
    }

    @Test
    void shouldRejectStateForDifferentUser() {
        String stateToken = stateService.generateState(userId);
        UUID differentUserId = UUID.randomUUID();
        assertFalse(stateService.validateAndConsumeState(stateToken, differentUserId));
    }

    @Test
    void shouldRejectInvalidOrEmptyState() {
        assertFalse(stateService.validateAndConsumeState(null, userId));
        assertFalse(stateService.validateAndConsumeState("", userId));
        assertFalse(stateService.validateAndConsumeState("non-existent-state", userId));
    }
}
