package com.azhost.github;

import com.azhost.entity.User;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.GitHubAuthorizationException;
import com.azhost.github.exception.GitHubConnectionNotFoundException;
import com.azhost.github.repository.GitHubConnectionRepository;
import com.azhost.github.security.GitHubSecurityPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitHubSecurityTest {

    private GitHubConnectionRepository connectionRepository;
    private GitHubSecurityPolicy securityPolicy;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        connectionRepository = mock(GitHubConnectionRepository.class);
        securityPolicy = new GitHubSecurityPolicy(connectionRepository);

        userId = UUID.randomUUID();
        user = new User("dev@azhost.dev", "hash", "Dev");
        user.setId(userId);
    }

    @Test
    void shouldValidateConnectedUserSuccessfully() {
        GitHubConnectionEntity connection = new GitHubConnectionEntity(user, 123L, "octocat", "url", "enc", "scope");
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.of(connection));

        GitHubConnectionEntity result = securityPolicy.validateUserConnection(user);
        assertNotNull(result);
        assertEquals("octocat", result.getGithubUsername());
    }

    @Test
    void shouldThrowExceptionWhenUserNotConnected() {
        when(connectionRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(GitHubConnectionNotFoundException.class, () ->
                securityPolicy.validateUserConnection(user));
    }

    @Test
    void shouldRejectAccessWhenUserDoesNotOwnProject() {
        UUID projectOwnerId = UUID.randomUUID();
        UUID callerUserId = UUID.randomUUID();

        assertThrows(GitHubAuthorizationException.class, () ->
                securityPolicy.validateProjectOwnership(projectOwnerId, callerUserId));
    }

    @Test
    void shouldAllowAccessWhenUserOwnsProject() {
        UUID ownerId = UUID.randomUUID();

        assertDoesNotThrow(() ->
                securityPolicy.validateProjectOwnership(ownerId, ownerId));
    }
}
