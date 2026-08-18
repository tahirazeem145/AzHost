package com.azhost.github;

import com.azhost.entity.User;
import com.azhost.github.dto.GitHubConnectionResponseDto;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.GitHubAuthenticationException;
import com.azhost.github.repository.GitHubConnectionRepository;
import com.azhost.github.security.GitHubOAuthStateService;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubOAuthServiceTest {

    private GitHubConnectionRepository connectionRepository;
    private UserRepository userRepository;
    private GitHubOAuthStateService stateService;
    private GitHubTokenEncryptor tokenEncryptor;
    private RestTemplate restTemplate;
    private GitHubOAuthService oAuthService;

    private User testUser;
    private static final String TEST_EMAIL = "developer@azhost.dev";

    @BeforeEach
    void setUp() {
        connectionRepository = mock(GitHubConnectionRepository.class);
        userRepository = mock(UserRepository.class);
        stateService = mock(GitHubOAuthStateService.class);
        tokenEncryptor = mock(GitHubTokenEncryptor.class);
        restTemplate = mock(RestTemplate.class);

        oAuthService = new GitHubOAuthService(
                connectionRepository, userRepository, stateService, tokenEncryptor, restTemplate
        );

        ReflectionTestUtils.setField(oAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(oAuthService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(oAuthService, "redirectUri", "http://localhost:8080/api/github/callback");
        ReflectionTestUtils.setField(oAuthService, "oauthUrl", "https://github.com/login/oauth");
        ReflectionTestUtils.setField(oAuthService, "apiBaseUrl", "https://api.github.com");

        testUser = new User(TEST_EMAIL, "hash", "Developer");
        testUser.setId(UUID.randomUUID());

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
    }

    @Test
    void shouldGenerateConnectUrlWithState() {
        when(stateService.generateState(testUser.getId())).thenReturn("mock-state-123");

        String url = oAuthService.generateConnectUrl(TEST_EMAIL);

        assertNotNull(url);
        assertTrue(url.contains("client_id=test-client-id"));
        assertTrue(url.contains("state=mock-state-123"));
    }

    @Test
    void shouldThrowExceptionWhenOAuthStateIsInvalid() {
        when(stateService.validateAndConsumeState("bad-state", testUser.getId())).thenReturn(false);

        assertThrows(GitHubAuthenticationException.class, () ->
                oAuthService.processCallback("code123", "bad-state", TEST_EMAIL));
    }

    @Test
    void shouldProcessCallbackAndSaveConnectionOnSuccess() {
        String state = "valid-state";
        String code = "valid-code";

        when(stateService.validateAndConsumeState(state, testUser.getId())).thenReturn(true);

        Map<String, Object> tokenResponseBody = Map.of("access_token", "gho_token123", "scope", "repo,user:email");
        when(restTemplate.postForEntity(contains("/access_token"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(tokenResponseBody, HttpStatus.OK));

        Map<String, Object> userResponseBody = Map.of("id", 12345678L, "login", "octocat", "avatar_url", "https://avatar.url");
        when(restTemplate.exchange(contains("/user"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(userResponseBody, HttpStatus.OK));

        when(tokenEncryptor.encrypt("gho_token123")).thenReturn("encrypted_gho_token");
        when(connectionRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());
        when(connectionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GitHubConnectionResponseDto dto = oAuthService.processCallback(code, state, TEST_EMAIL);

        assertTrue(dto.isConnected());
        assertEquals("octocat", dto.getGithubUsername());
        assertEquals("https://avatar.url", dto.getAvatarUrl());

        ArgumentCaptor<GitHubConnectionEntity> captor = ArgumentCaptor.forClass(GitHubConnectionEntity.class);
        verify(connectionRepository).save(captor.capture());
        assertEquals("encrypted_gho_token", captor.getValue().getEncryptedAccessToken());
    }

    @Test
    void shouldDisconnectGitHubAccount() {
        GitHubConnectionEntity connection = new GitHubConnectionEntity(testUser, 123L, "octocat", "url", "enc", "scope");
        when(connectionRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(connection));

        oAuthService.disconnect(TEST_EMAIL);

        verify(connectionRepository).delete(connection);
    }
}
