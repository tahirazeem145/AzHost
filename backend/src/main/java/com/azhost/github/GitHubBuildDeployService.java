package com.azhost.github;

import com.azhost.build.BuildStatus;
import com.azhost.dto.BuildResponseDto;
import com.azhost.dto.CreateDeploymentRequest;
import com.azhost.dto.DeploymentResponseDto;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectSourceType;
import com.azhost.repository.ProjectRepository;
import com.azhost.service.BuildService;
import com.azhost.service.DeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the complete GitHub → Build → Deploy pipeline.
 *
 * Design principles:
 * - Resolves the current commit SHA before building (branch → SHA for reproducibility)
 * - Updates the project's githubCommitSha before triggering the build
 * - Blocks/polls until the build is in a terminal state before deploying
 * - Only proceeds to deployment if build succeeded
 * - Delegates source acquisition, build, and deployment to the existing Phase 4/5 engines
 *
 * This service is the critical integration bridge that was missing between
 * the GitHub source provider and the existing deployment pipeline.
 */
@Service
public class GitHubBuildDeployService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubBuildDeployService.class);

    /** Maximum time to wait for a build before giving up (30 minutes) */
    private static final long BUILD_POLL_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);

    /** Interval between build status polls */
    private static final long BUILD_POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(5);

    private final BuildService buildService;
    private final DeploymentService deploymentService;
    private final GitHubRepositoryService repositoryService;
    private final ProjectRepository projectRepository;

    public GitHubBuildDeployService(
            BuildService buildService,
            DeploymentService deploymentService,
            GitHubRepositoryService repositoryService,
            ProjectRepository projectRepository
    ) {
        this.buildService = buildService;
        this.deploymentService = deploymentService;
        this.repositoryService = repositoryService;
        this.projectRepository = projectRepository;
    }

    /**
     * Trigger a build and deployment for the given project from its configured GitHub source.
     *
     * Flow:
     * 1. Optionally override the commit SHA (e.g. from a webhook push event)
     * 2. Update the project's githubCommitSha for reproducible builds
     * 3. Start build via BuildService (which calls SourceAcquisitionService → GitHubSourceProvider)
     * 4. Poll until build reaches a terminal state
     * 5. If build succeeded: create and submit deployment via DeploymentService
     * 6. If build failed: log and abort — do NOT create a deployment for a failed build
     *
     * @param project         the AZHost project (must have GITHUB source type + repository linked)
     * @param overrideCommitSha optional commit SHA from webhook push (null = use current project SHA or resolve from branch)
     * @param triggeredBy     human-readable label for log messages (e.g. "webhook", "manual")
     * @param userEmail       the AZHost user email (required by BuildService/DeploymentService)
     */
    public void triggerBuildAndDeploy(Project project, String overrideCommitSha, String triggeredBy, String userEmail) {
        UUID projectId = project.getId();
        logger.info("[GitHubBuildDeploy] Starting pipeline for project '{}' (ID: {}) triggered by: {}",
                project.getName(), projectId, triggeredBy);

        if (project.getSourceType() != ProjectSourceType.GITHUB) {
            throw new IllegalArgumentException("Project '" + project.getName() + "' is not configured with GITHUB source type");
        }

        if (project.getGithubRepositoryId() == null) {
            throw new IllegalArgumentException("Project '" + project.getName() + "' has no GitHub repository linked");
        }

        // Step 1: Resolve and persist the commit SHA
        String commitSha = resolveCommitSha(project, overrideCommitSha, userEmail);
        persistCommitSha(projectId, commitSha);

        logger.info("[GitHubBuildDeploy] Resolved commit SHA {} for project '{}'", commitSha, project.getName());

        // Step 2: Start build (synchronous: returns the build record, actual build is async internally)
        BuildResponseDto buildDto;
        try {
            buildDto = buildService.startBuild(projectId, userEmail);
        } catch (Exception e) {
            logger.error("[GitHubBuildDeploy] Failed to start build for project '{}': {}", project.getName(), e.getMessage(), e);
            throw new RuntimeException("Build initiation failed for project '" + project.getName() + "': " + e.getMessage(), e);
        }

        UUID buildId = buildDto.getId();
        logger.info("[GitHubBuildDeploy] Build {} started for project '{}'", buildId, project.getName());

        // Step 3: Wait for the build to complete (blocking poll)
        BuildStatus finalStatus = waitForBuildCompletion(projectId, buildId, userEmail);

        if (finalStatus != BuildStatus.SUCCESS) {
            logger.warn("[GitHubBuildDeploy] Build {} ended with status {} — NOT proceeding to deployment", buildId, finalStatus);
            throw new RuntimeException("Build failed with status " + finalStatus + " — deployment aborted for project '" + project.getName() + "'");
        }

        logger.info("[GitHubBuildDeploy] Build {} succeeded. Proceeding to deployment.", buildId);

        // Step 4: Create deployment using the successful build artifact
        try {
            CreateDeploymentRequest deploymentRequest = new CreateDeploymentRequest(buildId);
            DeploymentResponseDto deployment = deploymentService.createDeployment(projectId, deploymentRequest, userEmail);
            logger.info("[GitHubBuildDeploy] Deployment {} created for project '{}' from build {}",
                    deployment.getId(), project.getName(), buildId);
        } catch (Exception e) {
            logger.error("[GitHubBuildDeploy] Deployment creation failed for project '{}': {}", project.getName(), e.getMessage(), e);
            throw new RuntimeException("Deployment failed for project '" + project.getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Resolve the commit SHA to use for this deployment.
     * Priority: overrideCommitSha → project.githubCommitSha → resolve from branch via GitHub API
     */
    private String resolveCommitSha(Project project, String overrideCommitSha, String userEmail) {
        if (overrideCommitSha != null && !overrideCommitSha.isBlank()) {
            return overrideCommitSha;
        }

        // Try resolving from branch via GitHub API (most up-to-date)
        String branch = project.getGithubBranch();
        if (branch != null && !branch.isBlank()) {
            try {
                String resolvedSha = repositoryService.resolveCommitSha(userEmail, project.getGithubRepositoryId(), branch);
                if (resolvedSha != null && !resolvedSha.isBlank()) {
                    return resolvedSha;
                }
            } catch (Exception e) {
                logger.warn("[GitHubBuildDeploy] Could not resolve commit SHA from branch '{}' via GitHub API: {}", branch, e.getMessage());
            }
        }

        // Fall back to last known commit SHA on project
        if (project.getGithubCommitSha() != null && !project.getGithubCommitSha().isBlank()) {
            logger.warn("[GitHubBuildDeploy] Using stored commit SHA as fallback: {}", project.getGithubCommitSha());
            return project.getGithubCommitSha();
        }

        throw new IllegalStateException("Cannot determine commit SHA for project '" + project.getName() +
                "' — no branch configured and no stored commit SHA");
    }

    /**
     * Update the project's githubCommitSha so the build knows exactly which commit to use.
     */
    private void persistCommitSha(UUID projectId, String commitSha) {
        projectRepository.findById(projectId).ifPresent(p -> {
            p.setGithubCommitSha(commitSha);
            projectRepository.save(p);
        });
    }

    /**
     * Block and poll until the build reaches a terminal status (SUCCESS, FAILED, TIMEOUT, CANCELLED).
     * Returns the final BuildStatus so the caller can decide whether to proceed to deployment.
     *
     * This ensures we never create a deployment from a failed or incomplete build.
     */
    private BuildStatus waitForBuildCompletion(UUID projectId, UUID buildId, String userEmail) {
        long startMs = System.currentTimeMillis();

        while (true) {
            try {
                Thread.sleep(BUILD_POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("[GitHubBuildDeploy] Build poll interrupted for build {}", buildId);
                return BuildStatus.CANCELLED;
            }

            BuildStatus status = queryBuildStatus(projectId, buildId, userEmail);
            logger.debug("[GitHubBuildDeploy] Build {} status: {}", buildId, status);

            if (status.isTerminal()) {
                return status;
            }

            long elapsedMs = System.currentTimeMillis() - startMs;
            if (elapsedMs > BUILD_POLL_TIMEOUT_MS) {
                logger.error("[GitHubBuildDeploy] Build {} timed out after {} minutes", buildId, BUILD_POLL_TIMEOUT_MS / 60000);
                return BuildStatus.TIMEOUT;
            }
        }
    }

    /**
     * Query the current status of a build directly from the database.
     * Uses the build repository to avoid hitting the API layer.
     */
    private BuildStatus queryBuildStatus(UUID projectId, UUID buildId, String userEmail) {
        try {
            BuildResponseDto buildDto = buildService.getBuildById(projectId, buildId, userEmail);
            return buildDto.getStatus();
        } catch (Exception e) {
            logger.warn("[GitHubBuildDeploy] Could not query build status for {}: {}", buildId, e.getMessage());
            return BuildStatus.QUEUED; // Treat query failures as still in progress
        }
    }
}
