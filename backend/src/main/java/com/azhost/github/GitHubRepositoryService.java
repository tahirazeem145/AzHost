package com.azhost.github;

import com.azhost.entity.User;
import com.azhost.github.dto.GitHubBranchDto;
import com.azhost.github.dto.GitHubCommitDto;
import com.azhost.github.dto.GitHubRepositoryDto;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.*;
import com.azhost.github.security.GitHubSecurityPolicy;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GitHubRepositoryService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubRepositoryService.class);

    private final UserRepository userRepository;
    private final GitHubSecurityPolicy securityPolicy;
    private final GitHubTokenEncryptor tokenEncryptor;
    private final RestTemplate restTemplate;

    @Value("${azhost.github.api-base-url:https://api.github.com}")
    private String apiBaseUrl;

    public GitHubRepositoryService(
            UserRepository userRepository,
            GitHubSecurityPolicy securityPolicy,
            GitHubTokenEncryptor tokenEncryptor,
            RestTemplate restTemplate
    ) {
        this.userRepository = userRepository;
        this.securityPolicy = securityPolicy;
        this.tokenEncryptor = tokenEncryptor;
        this.restTemplate = restTemplate;
    }

    public List<GitHubRepositoryDto> getUserRepositories(String userEmail) {
        User user = getUser(userEmail);
        GitHubConnectionEntity connection = securityPolicy.validateUserConnection(user);
        String decryptedToken = tokenEncryptor.decrypt(connection.getEncryptedAccessToken());

        String url = apiBaseUrl + "/user/repos?per_page=100&sort=updated&type=all";
        HttpEntity<Void> requestEntity = createAuthEntity(decryptedToken);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, List.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<GitHubRepositoryDto> result = new ArrayList<>();
                for (Object item : response.getBody()) {
                    if (item instanceof Map repoMap) {
                        Long id = ((Number) repoMap.get("id")).longValue();
                        String name = (String) repoMap.get("name");
                        String fullName = (String) repoMap.get("full_name");
                        Boolean isPrivate = (Boolean) repoMap.get("private");
                        String defaultBranch = (String) repoMap.get("default_branch");
                        String htmlUrl = (String) repoMap.get("html_url");
                        String updatedAt = (String) repoMap.get("updated_at");

                        result.add(new GitHubRepositoryDto(id, name, fullName, Boolean.TRUE.equals(isPrivate), defaultBranch, htmlUrl, updatedAt));
                    }
                }
                return result;
            }
            return Collections.emptyList();
        } catch (HttpClientErrorException ex) {
            handleHttpClientException(ex);
            return Collections.emptyList();
        } catch (HttpServerErrorException ex) {
            throw new GitHubSourceAcquisitionException("GitHub API server error: " + ex.getMessage(), ex);
        } catch (Exception e) {
            logger.error("Error listing GitHub repositories for user '{}'", userEmail, e);
            throw new GitHubSourceAcquisitionException("Failed to fetch repositories from GitHub: " + e.getMessage(), e);
        }
    }

    public List<GitHubBranchDto> getRepositoryBranches(String userEmail, Long repositoryId) {
        User user = getUser(userEmail);
        GitHubConnectionEntity connection = securityPolicy.validateUserConnection(user);
        String decryptedToken = tokenEncryptor.decrypt(connection.getEncryptedAccessToken());

        String url = apiBaseUrl + "/repositories/" + repositoryId + "/branches?per_page=100";
        HttpEntity<Void> requestEntity = createAuthEntity(decryptedToken);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, List.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<GitHubBranchDto> result = new ArrayList<>();
                for (Object item : response.getBody()) {
                    if (item instanceof Map branchMap) {
                        String name = (String) branchMap.get("name");
                        Boolean isProtected = (Boolean) branchMap.get("protected");
                        result.add(new GitHubBranchDto(name, Boolean.TRUE.equals(isProtected)));
                    }
                }
                return result;
            }
            return Collections.emptyList();
        } catch (HttpClientErrorException ex) {
            handleHttpClientException(ex);
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error listing branches for repository '{}'", repositoryId, e);
            throw new GitHubSourceAcquisitionException("Failed to fetch branches from GitHub: " + e.getMessage(), e);
        }
    }

    public GitHubRepositoryDto getRepositoryDetails(String userEmail, Long repositoryId) {
        User user = getUser(userEmail);
        GitHubConnectionEntity connection = securityPolicy.validateUserConnection(user);
        String decryptedToken = tokenEncryptor.decrypt(connection.getEncryptedAccessToken());

        String url = apiBaseUrl + "/repositories/" + repositoryId;
        HttpEntity<Void> requestEntity = createAuthEntity(decryptedToken);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map repoMap = response.getBody();
                Long id = ((Number) repoMap.get("id")).longValue();
                String name = (String) repoMap.get("name");
                String fullName = (String) repoMap.get("full_name");
                Boolean isPrivate = (Boolean) repoMap.get("private");
                String defaultBranch = (String) repoMap.get("default_branch");
                String htmlUrl = (String) repoMap.get("html_url");
                String updatedAt = (String) repoMap.get("updated_at");

                return new GitHubRepositoryDto(id, name, fullName, Boolean.TRUE.equals(isPrivate), defaultBranch, htmlUrl, updatedAt);
            }
            throw new GitHubRepositoryNotFoundException("GitHub repository not found with ID: " + repositoryId);
        } catch (HttpClientErrorException ex) {
            handleHttpClientException(ex);
            throw new GitHubRepositoryNotFoundException("GitHub repository not found with ID: " + repositoryId);
        }
    }

    public String resolveCommitSha(String userEmail, Long repositoryId, String ref) {
        User user = getUser(userEmail);
        GitHubConnectionEntity connection = securityPolicy.validateUserConnection(user);
        String decryptedToken = tokenEncryptor.decrypt(connection.getEncryptedAccessToken());

        String url = apiBaseUrl + "/repositories/" + repositoryId + "/commits/" + ref;
        HttpEntity<Void> requestEntity = createAuthEntity(decryptedToken);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map commitMap = response.getBody();
                String sha = (String) commitMap.get("sha");
                if (sha != null && !sha.isBlank()) {
                    return sha;
                }
            }
            throw new GitHubSourceAcquisitionException("Unable to resolve commit SHA for reference: " + ref);
        } catch (HttpClientErrorException ex) {
            handleHttpClientException(ex);
            throw new GitHubSourceAcquisitionException("Failed to resolve commit SHA: " + ex.getMessage());
        } catch (Exception e) {
            if (e instanceof GitHubSourceAcquisitionException gse) throw gse;
            logger.error("Error resolving commit SHA for repo {} ref {}", repositoryId, ref, e);
            throw new GitHubSourceAcquisitionException("Error resolving commit SHA from GitHub: " + e.getMessage(), e);
        }
    }

    private HttpEntity<Void> createAuthEntity(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        return new HttpEntity<>(headers);
    }

    private void handleHttpClientException(HttpClientErrorException ex) {
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            throw new GitHubAuthenticationException("GitHub access token is invalid or has expired", ex);
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new GitHubAuthorizationException("Access denied by GitHub or API rate limit exceeded");
        } else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new GitHubRepositoryNotFoundException("GitHub repository or resource was not found");
        } else if (ex.getStatusCode().value() == 429) {
            throw new GitHubAuthorizationException("GitHub API rate limit exceeded. Please try again later.");
        } else {
            throw new GitHubSourceAcquisitionException("GitHub API request failed: " + ex.getMessage());
        }
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
