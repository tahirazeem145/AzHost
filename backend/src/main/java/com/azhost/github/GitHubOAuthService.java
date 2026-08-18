package com.azhost.github;

import com.azhost.entity.User;
import com.azhost.github.dto.GitHubConnectionResponseDto;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.GitHubAuthenticationException;
import com.azhost.github.repository.GitHubConnectionRepository;
import com.azhost.github.security.GitHubOAuthStateService;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GitHubOAuthService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuthService.class);

    private final GitHubConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final GitHubOAuthStateService stateService;
    private final GitHubTokenEncryptor tokenEncryptor;
    private final RestTemplate restTemplate;

    @Value("${azhost.github.client-id}")
    private String clientId;

    @Value("${azhost.github.client-secret}")
    private String clientSecret;

    @Value("${azhost.github.redirect-uri}")
    private String redirectUri;

    @Value("${azhost.github.oauth-url:https://github.com/login/oauth}")
    private String oauthUrl;

    @Value("${azhost.github.api-base-url:https://api.github.com}")
    private String apiBaseUrl;

    public GitHubOAuthService(
            GitHubConnectionRepository connectionRepository,
            UserRepository userRepository,
            GitHubOAuthStateService stateService,
            GitHubTokenEncryptor tokenEncryptor,
            RestTemplate restTemplate
    ) {
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.stateService = stateService;
        this.tokenEncryptor = tokenEncryptor;
        this.restTemplate = restTemplate;
    }

    public String generateConnectUrl(String userEmail) {
        User user = getUser(userEmail);
        String state = stateService.generateState(user.getId());
        return String.format("%s/authorize?client_id=%s&redirect_uri=%s&scope=repo,user:email&state=%s",
                oauthUrl, clientId, redirectUri, state);
    }

    @Transactional
    public GitHubConnectionResponseDto processCallback(String code, String state, String userEmail) {
        User user = getUser(userEmail);

        if (!stateService.validateAndConsumeState(state, user.getId())) {
            throw new GitHubAuthenticationException("OAuth state verification failed. The state may be invalid, expired, or reused.");
        }

        if (code == null || code.isBlank()) {
            throw new GitHubAuthenticationException("Authorization code was not provided by GitHub callback.");
        }

        // Exchange code for OAuth access token
        String tokenUrl = oauthUrl + "/access_token";
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        Map<String, String> body = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri
        );

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> tokenResponse;
        try {
            tokenResponse = restTemplate.postForEntity(tokenUrl, requestEntity, Map.class);
        } catch (Exception e) {
            logger.error("Failed to execute OAuth token exchange with GitHub", e);
            throw new GitHubAuthenticationException("Failed to communicate with GitHub OAuth token server", e);
        }

        if (!tokenResponse.getStatusCode().is2xxSuccessful() || tokenResponse.getBody() == null) {
            throw new GitHubAuthenticationException("GitHub OAuth token endpoint returned an error response");
        }

        Map responseMap = tokenResponse.getBody();
        String accessToken = (String) responseMap.get("access_token");
        String scope = (String) responseMap.get("scope");

        if (accessToken == null || accessToken.isBlank()) {
            String errorDesc = (String) responseMap.get("error_description");
            throw new GitHubAuthenticationException("GitHub OAuth token exchange failed: " + (errorDesc != null ? errorDesc : "No access token received"));
        }

        // Fetch GitHub user profile
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        userHeaders.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        HttpEntity<Void> userRequestEntity = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userProfileResponse;
        try {
            userProfileResponse = restTemplate.exchange(apiBaseUrl + "/user", HttpMethod.GET, userRequestEntity, Map.class);
        } catch (Exception e) {
            logger.error("Failed to fetch GitHub user profile", e);
            throw new GitHubAuthenticationException("Failed to fetch GitHub user profile with access token", e);
        }

        if (!userProfileResponse.getStatusCode().is2xxSuccessful() || userProfileResponse.getBody() == null) {
            throw new GitHubAuthenticationException("GitHub API returned error when fetching user profile");
        }

        Map profileMap = userProfileResponse.getBody();
        Number githubUserIdNum = (Number) profileMap.get("id");
        String githubUsername = (String) profileMap.get("login");
        String avatarUrl = (String) profileMap.get("avatar_url");

        if (githubUserIdNum == null || githubUsername == null) {
            throw new GitHubAuthenticationException("GitHub user profile response missing required user identity fields");
        }

        Long githubUserId = githubUserIdNum.longValue();
        String encryptedToken = tokenEncryptor.encrypt(accessToken);

        // Save or update GitHub connection entity
        GitHubConnectionEntity connection = connectionRepository.findByUserId(user.getId())
                .orElseGet(() -> new GitHubConnectionEntity());

        connection.setUser(user);
        connection.setGithubUserId(githubUserId);
        connection.setGithubUsername(githubUsername);
        connection.setAvatarUrl(avatarUrl);
        connection.setEncryptedAccessToken(encryptedToken);
        connection.setScopes(scope);

        GitHubConnectionEntity saved = connectionRepository.save(connection);
        logger.info("Successfully established GitHub connection for AZHost user '{}' (GitHub: {})", userEmail, githubUsername);

        return new GitHubConnectionResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public GitHubConnectionResponseDto getConnection(String userEmail) {
        User user = getUser(userEmail);
        return connectionRepository.findByUserId(user.getId())
                .map(GitHubConnectionResponseDto::new)
                .orElseGet(GitHubConnectionResponseDto::disconnected);
    }

    @Transactional
    public void disconnect(String userEmail) {
        User user = getUser(userEmail);
        connectionRepository.findByUserId(user.getId()).ifPresent(connection -> {
            connectionRepository.delete(connection);
            logger.info("Disconnected GitHub connection for user '{}'", userEmail);
        });
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
