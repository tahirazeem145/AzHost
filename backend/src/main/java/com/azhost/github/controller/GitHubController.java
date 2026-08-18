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
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GitHubController {

    private final GitHubOAuthService oauthService;
    private final GitHubRepositoryService repositoryService;
    private final GitHubSecurityPolicy securityPolicy;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public GitHubController(
            GitHubOAuthService oauthService,
            GitHubRepositoryService repositoryService,
            GitHubSecurityPolicy securityPolicy,
            ProjectRepository projectRepository,
            UserRepository userRepository
    ) {
        this.oauthService = oauthService;
        this.repositoryService = repositoryService;
        this.securityPolicy = securityPolicy;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/github/connect")
    public ResponseEntity<Map<String, String>> connectGitHub() {
        String authUrl = oauthService.generateConnectUrl(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    @GetMapping("/github/callback")
    public RedirectView handleCallback(@RequestParam String code, @RequestParam String state) {
        oauthService.processCallback(code, state, DevUserInitializer.DEV_USER_EMAIL);
        return new RedirectView("/settings?github=connected");
    }

    @GetMapping("/github/connection")
    public ResponseEntity<GitHubConnectionResponseDto> getConnectionStatus() {
        GitHubConnectionResponseDto dto = oauthService.getConnection(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/github/connection")
    public ResponseEntity<Void> disconnectGitHub() {
        oauthService.disconnect(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/github/repositories")
    public ResponseEntity<List<GitHubRepositoryDto>> getRepositories() {
        List<GitHubRepositoryDto> repos = repositoryService.getUserRepositories(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(repos);
    }

    @GetMapping("/github/repositories/{repoId}/branches")
    public ResponseEntity<List<GitHubBranchDto>> getBranches(@PathVariable Long repoId) {
        List<GitHubBranchDto> branches = repositoryService.getRepositoryBranches(DevUserInitializer.DEV_USER_EMAIL, repoId);
        return ResponseEntity.ok(branches);
    }

    @PostMapping("/projects/{projectId}/github")
    public ResponseEntity<ProjectResponseDto> linkProjectGitHub(
            @PathVariable UUID projectId,
            @Valid @RequestBody LinkGitHubRequestDto request
    ) {
        User user = getUser(DevUserInitializer.DEV_USER_EMAIL);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        // Security check: ensure GitHub connection exists
        securityPolicy.validateUserConnection(user);

        // Fetch repository details to obtain authoritative repo name and html_url
        GitHubRepositoryDto repoDetails = repositoryService.getRepositoryDetails(DevUserInitializer.DEV_USER_EMAIL, request.getRepositoryId());

        // Resolve current commit SHA for specified branch
        String commitSha = repositoryService.resolveCommitSha(DevUserInitializer.DEV_USER_EMAIL, request.getRepositoryId(), request.getBranch());

        project.setSourceType(ProjectSourceType.GITHUB);
        project.setGithubRepositoryId(request.getRepositoryId());
        project.setGithubRepositoryName(repoDetails.getFullName());
        project.setGithubBranch(request.getBranch());
        project.setGithubCommitSha(commitSha);
        project.setRepositoryUrl(repoDetails.getHtmlUrl());
        project.setRepositoryBranch(request.getBranch());

        Project saved = projectRepository.save(project);
        return ResponseEntity.ok(new ProjectResponseDto(saved));
    }

    @DeleteMapping("/projects/{projectId}/github")
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

        Project saved = projectRepository.save(project);
        return ResponseEntity.ok(new ProjectResponseDto(saved));
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
