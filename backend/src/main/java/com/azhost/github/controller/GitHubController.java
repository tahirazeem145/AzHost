package com.azhost.github.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.ProjectResponseDto;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.github.GitHubOAuthService;
import com.azhost.github.GitHubRepositoryService;
import com.azhost.github.dto.*;
import com.azhost.github.repository.GitHubConnectionRepository;
import com.azhost.github.security.GitHubSecurityPolicy;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GitHub integration controller.
 *
 * Endpoints are available under two prefixes for compatibility:
 *   /api/github/**            — legacy paths (frontend compatibility)
 *   /api/integrations/github/**  — Phase 7 specification paths
 */
@RestController
public class GitHubController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubController.class);

    private final GitHubOAuthService oauthService;
    private final GitHubRepositoryService repositoryService;
    private final GitHubSecurityPolicy securityPolicy;
    private final GitHubTokenEncryptor tokenEncryptor;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Value("${azhost.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public GitHubController(
            GitHubOAuthService oauthService,
            GitHubRepositoryService repositoryService,
            GitHubSecurityPolicy securityPolicy,
            GitHubTokenEncryptor tokenEncryptor,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.oauthService = oauthService;
        this.repositoryService = repositoryService;
        this.securityPolicy = securityPolicy;
        this.tokenEncryptor = tokenEncryptor;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    // ── OAuth Connect Flow ─────────────────────────────────────────────────────

    @GetMapping({"/api/github/connect", "/api/integrations/github/connect"})
    public ResponseEntity<Map<String, String>> connectGitHub() {
        logger.info("GitHub OAuth connect initiated");
        String authUrl = oauthService.generateConnectUrl(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @GetMapping({"/api/github/callback", "/api/integrations/github/callback"})
    public RedirectView handleCallback(@RequestParam String code, @RequestParam String state) {
        logger.info("GitHub OAuth callback received");
        oauthService.processCallback(code, state, DevUserInitializer.DEV_USER_EMAIL);
        return new RedirectView(frontendBaseUrl + "/settings?github=connected");
    }

    // ── Connection Status ──────────────────────────────────────────────────────

    @GetMapping({"/api/github/connection", "/api/integrations/github/status"})
    public ResponseEntity<GitHubConnectionResponseDto> getConnectionStatus() {
        GitHubConnectionResponseDto dto = oauthService.getConnection(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping({"/api/github/connection", "/api/integrations/github"})
    public ResponseEntity<Void> disconnectGitHub() {
        logger.info("GitHub disconnect requested");
        oauthService.disconnect(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.noContent().build();
    }

    // ── Repository Discovery ───────────────────────────────────────────────────

    @GetMapping({"/api/github/repositories", "/api/integrations/github/repositories"})
    public ResponseEntity<List<GitHubRepositoryDto>> getRepositories() {
        List<GitHubRepositoryDto> repos = repositoryService.getUserRepositories(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(repos);
    }

    /**
     * List branches by repository ID (existing path, used by frontend).
     */
    @GetMapping("/api/github/repositories/{repoId}/branches")
    public ResponseEntity<List<GitHubBranchDto>> getBranchesByRepoId(@PathVariable Long repoId) {
        List<GitHubBranchDto> branches = repositoryService.getRepositoryBranches(DevUserInitializer.DEV_USER_EMAIL, repoId);
        return ResponseEntity.ok(branches);
    }

    /**
     * List branches by owner/repo path (Phase 7 spec path).
     * Resolves the repository to an ID using the GitHub API, then fetches branches.
     */
    @GetMapping("/api/integrations/github/repositories/{owner}/{repo}/branches")
    public ResponseEntity<List<GitHubBranchDto>> getBranchesByOwnerRepo(
            @PathVariable String owner,
            @PathVariable String repo
    ) {
        // Validate owner and repo name to prevent injection
        validateRepoIdentifier(owner, "owner");
        validateRepoIdentifier(repo, "repository");

        // Look up the repository to get its ID (server-side — never trust client-submitted ID)
        List<GitHubRepositoryDto> repos = repositoryService.getUserRepositories(DevUserInitializer.DEV_USER_EMAIL);
        String fullName = owner + "/" + repo;
        GitHubRepositoryDto repoDto = repos.stream()
                .filter(r -> fullName.equalsIgnoreCase(r.getFullName()))
                .findFirst()
                .orElseThrow(() -> new com.azhost.github.exception.GitHubRepositoryNotFoundException(
                        "Repository '" + fullName + "' not found in your connected GitHub account"));

        List<GitHubBranchDto> branches = repositoryService.getRepositoryBranches(DevUserInitializer.DEV_USER_EMAIL, repoDto.getId());
        return ResponseEntity.ok(branches);
    }

    // ── Project → GitHub Repository Linking ───────────────────────────────────

    @PostMapping("/api/projects/{projectId}/github")
    public ResponseEntity<ProjectResponseDto> linkProjectGitHub(
            @PathVariable UUID projectId,
            @Valid @RequestBody LinkGitHubRequestDto request
    ) {
        User user = getUser(DevUserInitializer.DEV_USER_EMAIL);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        // Security: ensure GitHub connection exists for this user
        securityPolicy.validateUserConnection(user);

        // Server-side validation: fetch authoritative repository details (never trust client-submitted fullName)
        GitHubRepositoryDto repoDetails = repositoryService.getRepositoryDetails(DevUserInitializer.DEV_USER_EMAIL, request.getRepositoryId());

        // Resolve current commit SHA for the specified branch (server-side)
        String commitSha = repositoryService.resolveCommitSha(DevUserInitializer.DEV_USER_EMAIL, request.getRepositoryId(), request.getBranch());

        project.setSourceType(ProjectSourceType.GITHUB);
        project.setGithubRepositoryId(request.getRepositoryId());
        project.setGithubRepositoryName(repoDetails.getFullName());
        project.setGithubBranch(request.getBranch());
        project.setGithubCommitSha(commitSha);
        project.setRepositoryUrl(repoDetails.getHtmlUrl());
        project.setRepositoryBranch(request.getBranch());

        // Default auto-deploy branch to the configured branch if not set
        if (project.getAutoDeployBranch() == null || project.getAutoDeployBranch().isBlank()) {
            project.setAutoDeployBranch(request.getBranch());
        }

        Project saved = projectRepository.save(project);
        logger.info("Linked project '{}' (ID: {}) to GitHub repository '{}'",
                saved.getName(), saved.getId(), repoDetails.getFullName());
        return ResponseEntity.ok(new ProjectResponseDto(saved));
    }

    @DeleteMapping("/api/projects/{projectId}/github")
    public ResponseEntity<ProjectResponseDto> unlinkProjectGitHub(@PathVariable UUID projectId) {
        User user = getUser(DevUserInitializer.DEV_USER_EMAIL);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        project.setSourceType(ProjectSourceType.LOCAL);
        project.setGithubRepositoryId(null);
        project.setGithubRepositoryName(null);
        project.setGithubBranch(null);
        project.setGithubCommitSha(null);
        project.setRepositoryUrl(null);
        project.setRepositoryBranch(null);
        // Preserve auto-deploy settings so they can be reused if re-linked

        Project saved = projectRepository.save(project);
        logger.info("Unlinked GitHub repository from project '{}' (ID: {})", saved.getName(), saved.getId());
        return ResponseEntity.ok(new ProjectResponseDto(saved));
    }

    // ── Webhook Secret Management ──────────────────────────────────────────────

    /**
     * Configure or rotate the per-project GitHub webhook secret.
     * The secret is encrypted before storage using the same AES-256-GCM approach
     * used for OAuth tokens — it is never returned to clients.
     *
     * Body: { "webhookSecret": "your-secret-here" }
     */
    @PostMapping("/api/projects/{projectId}/github/webhook-secret")
    public ResponseEntity<Map<String, String>> setWebhookSecret(
            @PathVariable UUID projectId,
            @RequestBody Map<String, String> body
    ) {
        String secret = body.get("webhookSecret");
        if (secret == null || secret.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "webhookSecret must not be blank"));
        }

        User user = getUser(DevUserInitializer.DEV_USER_EMAIL);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        // Encrypt and store — never persist plaintext
        String encryptedSecret = tokenEncryptor.encrypt(secret);
        project.setEncryptedWebhookSecret(encryptedSecret);
        projectRepository.save(project);

        logger.info("Webhook secret updated for project '{}' (ID: {})", project.getName(), projectId);
        return ResponseEntity.ok(Map.of("message", "Webhook secret saved successfully"));
    }

    // ── Auto-Deploy Settings ───────────────────────────────────────────────────

    /**
     * Update auto-deploy settings for a project.
     * Body: { "autoDeploy": true, "autoDeployBranch": "main" }
     */
    @PatchMapping("/api/projects/{projectId}/github/auto-deploy")
    public ResponseEntity<ProjectResponseDto> updateAutoDeploySettings(
            @PathVariable UUID projectId,
            @RequestBody Map<String, Object> body
    ) {
        User user = getUser(DevUserInitializer.DEV_USER_EMAIL);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        if (body.containsKey("autoDeploy")) {
            Object val = body.get("autoDeploy");
            if (val instanceof Boolean b) {
                project.setAutoDeploy(b);
            }
        }
        if (body.containsKey("autoDeployBranch")) {
            Object val = body.get("autoDeployBranch");
            if (val instanceof String s && !s.isBlank()) {
                validateRepoIdentifier(s, "autoDeployBranch");
                project.setAutoDeployBranch(s.trim());
            }
        }

        Project saved = projectRepository.save(project);
        logger.info("Auto-deploy settings updated for project '{}': enabled={}, branch={}",
                saved.getName(), saved.isAutoDeploy(), saved.getAutoDeployBranch());
        return ResponseEntity.ok(new ProjectResponseDto(saved));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }

    /**
     * Validate that a repository owner/name/branch identifier does not contain
     * shell-injection characters. Restricts to safe alphanumeric + common SCM characters.
     */
    private void validateRepoIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        // Allow alphanumeric, hyphens, underscores, dots, forward slashes (for branch names like feature/x)
        if (!value.matches("^[a-zA-Z0-9._/\\-]+$")) {
            throw new IllegalArgumentException("Invalid characters in " + fieldName + ": '" + value + "'");
        }
        if (value.contains("..")) {
            throw new IllegalArgumentException("Path traversal not allowed in " + fieldName);
        }
    }
}
