package com.azhost.github;

import com.azhost.entity.User;
import com.azhost.github.dto.GitHubBranchDto;
import com.azhost.github.dto.GitHubRepositoryDto;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.GitHubAuthenticationException;
import com.azhost.github.exception.GitHubAuthorizationException;
import com.azhost.github.exception.GitHubRepositoryNotFoundException;
import com.azhost.github.security.GitHubSecurityPolicy;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubRepositoryServiceTest {

    private UserRepository userRepository;
    private GitHubSecurityPolicy securityPolicy;
    private GitHubTokenEncryptor tokenEncryptor;
    private RestTemplate restTemplate;
    private GitHubRepositoryService repositoryService;

    private User testUser;
    private GitHubConnectionEntity connection;
    private static final String TEST_EMAIL = "developer@azhost.dev";

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        securityPolicy = mock(GitHubSecurityPolicy.class);
        tokenEncryptor = mock(GitHubTokenEncryptor.class);
        restTemplate = mock(RestTemplate.class);

        repositoryService = new GitHubRepositoryService(userRepository, securityPolicy, tokenEncryptor, restTemplate);
        ReflectionTestUtils.setField(repositoryService, "apiBaseUrl", "https://api.github.com");

        testUser = new User(TEST_EMAIL, "hash", "Dev");
        testUser.setId(UUID.randomUUID());

        connection = new GitHubConnectionEntity(testUser, 100L, "testuser", "avatar", "enc-token", "repo");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(securityPolicy.validateUserConnection(testUser)).thenReturn(connection);
        when(tokenEncryptor.decrypt("enc-token")).thenReturn("decrypted-token");
    }

    @Test
    void shouldListUserRepositoriesSuccessfully() {
        List<Map<String, Object>> mockResponse = List.of(
                Map.of(
                        "id", 12345L,
                        "name", "TripNest",
                        "full_name", "testuser/TripNest",
                        "private", true,
                        "default_branch", "main",
                        "html_url", "https://github.com/testuser/TripNest",
                        "updated_at", "2026-08-18T12:00:00Z"
                )
        );

        when(restTemplate.exchange(contains("/user/repos"), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        List<GitHubRepositoryDto> repos = repositoryService.getUserRepositories(TEST_EMAIL);

        assertEquals(1, repos.size());
        assertEquals("TripNest", repos.get(0).getName());
        assertEquals("testuser/TripNest", repos.get(0).getFullName());
        assertTrue(repos.get(0).isPrivate());
        assertEquals("main", repos.get(0).getDefaultBranch());
    }

    @Test
    void shouldListRepositoryBranchesSuccessfully() {
        List<Map<String, Object>> mockBranches = List.of(
                Map.of("name", "main", "protected", true),
                Map.of("name", "develop", "protected", false)
        );

        when(restTemplate.exchange(contains("/repositories/12345/branches"), eq(HttpMethod.GET), any(HttpEntity.class), eq(List.class)))
                .thenReturn(new ResponseEntity<>(mockBranches, HttpStatus.OK));

        List<GitHubBranchDto> branches = repositoryService.getRepositoryBranches(TEST_EMAIL, 12345L);

        assertEquals(2, branches.size());
        assertEquals("main", branches.get(0).getName());
        assertTrue(branches.get(0).isProtected());
        assertEquals("develop", branches.get(1).getName());
        assertFalse(branches.get(1).isProtected());
    }

    @Test
    void shouldResolveCommitShaSuccessfully() {
        Map<String, Object> mockCommit = Map.of("sha", "abc123def456789012345678901234567890abcd");

        when(restTemplate.exchange(contains("/repositories/12345/commits/main"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(mockCommit, HttpStatus.OK));

        String sha = repositoryService.resolveCommitSha(TEST_EMAIL, 12345L, "main");

        assertEquals("abc123def456789012345678901234567890abcd", sha);
    }

    @Test
    void shouldHandle401UnauthorizedFromGitHub() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(List.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThrows(GitHubAuthenticationException.class, () -> repositoryService.getUserRepositories(TEST_EMAIL));
    }

    @Test
    void shouldHandle404NotFoundFromGitHub() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(GitHubRepositoryNotFoundException.class, () -> repositoryService.getRepositoryDetails(TEST_EMAIL, 99999L));
    }

    @Test
    void shouldHandle429RateLimitExceededFromGitHub() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(List.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], null));

        assertThrows(GitHubAuthorizationException.class, () -> repositoryService.getUserRepositories(TEST_EMAIL));
    }
}
