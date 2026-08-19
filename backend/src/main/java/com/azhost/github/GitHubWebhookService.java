package com.azhost.github;

import com.azhost.config.DevUserInitializer;
import com.azhost.entity.Project;
import com.azhost.github.entity.GitHubWebhookDeliveryEntity;
import com.azhost.github.repository.GitHubWebhookDeliveryRepository;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.github.security.GitHubWebhookSignatureVerifier;
import com.azhost.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles GitHub webhook events.
 *
 * Responsibilities:
 * 1. Signature verification (delegated to GitHubWebhookSignatureVerifier)
 * 2. Event type filtering (only process 'push' events)
 * 3. Project identification by GitHub repository ID
 * 4. Idempotency: deduplicate by X-GitHub-Delivery ID
 * 5. Auto-deploy checks (enabled + branch match)
 * 6. Delegate to GitHubBuildDeployService for the actual pipeline
 *
 * Security note: The webhook secret is stored encrypted in the project.
 * We decrypt it here only for signature verification, never log it.
 */
@Service
public class GitHubWebhookService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookService.class);

    private static final String EVENT_PUSH = "push";
    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    private final GitHubWebhookSignatureVerifier signatureVerifier;
    private final GitHubTokenEncryptor tokenEncryptor;
    private final ProjectRepository projectRepository;
    private final GitHubWebhookDeliveryRepository deliveryRepository;
    private final GitHubBuildDeployService buildDeployService;

    public GitHubWebhookService(
            GitHubWebhookSignatureVerifier signatureVerifier,
            GitHubTokenEncryptor tokenEncryptor,
            ProjectRepository projectRepository,
            GitHubWebhookDeliveryRepository deliveryRepository,
            GitHubBuildDeployService buildDeployService
    ) {
        this.signatureVerifier = signatureVerifier;
        this.tokenEncryptor = tokenEncryptor;
        this.projectRepository = projectRepository;
        this.deliveryRepository = deliveryRepository;
        this.buildDeployService = buildDeployService;
    }

    /**
     * Validate the webhook signature for a given project.
     * Called synchronously BEFORE returning a response to GitHub.
     *
     * @return true if signature is valid, false otherwise
     */
    public boolean validateSignature(String signatureHeader, byte[] rawPayload, Project project) {
        String encryptedSecret = project.getEncryptedWebhookSecret();
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            logger.warn("Project '{}' has no webhook secret configured — rejecting webhook", project.getName());
            return false;
        }

        String plainSecret;
        try {
            plainSecret = tokenEncryptor.decrypt(encryptedSecret);
        } catch (Exception e) {
            logger.error("Failed to decrypt webhook secret for project '{}' — cannot verify signature", project.getName());
            return false;
        }

        return signatureVerifier.verify(signatureHeader, rawPayload, plainSecret);
    }

    /**
     * Validate signature against a global/default webhook secret (for projects without per-project secrets).
     * Used when the event arrives but we haven't yet identified the project.
     */
    public boolean validateGlobalSignature(String signatureHeader, byte[] rawPayload, String globalWebhookSecret) {
        if (globalWebhookSecret == null || globalWebhookSecret.isBlank()) {
            return false;
        }
        return signatureVerifier.verify(signatureHeader, rawPayload, globalWebhookSecret);
    }

    /**
     * Find the AZHost project associated with a GitHub repository ID.
     */
    @Transactional(readOnly = true)
    public Optional<Project> findProjectByRepositoryId(Long githubRepositoryId) {
        if (githubRepositoryId == null) {
            return Optional.empty();
        }
        List<Project> projects = projectRepository.findAllByGithubRepositoryId(githubRepositoryId);
        if (projects.isEmpty()) {
            return Optional.empty();
        }
        if (projects.size() > 1) {
            logger.warn("Multiple projects found for GitHub repository ID {} — using first match", githubRepositoryId);
        }
        return Optional.of(projects.get(0));
    }

    /**
     * Process a validated GitHub push event asynchronously.
     * This method is called AFTER signature verification and 202 response is already sent.
     *
     * Returns a human-readable outcome string for logging.
     */
    @Async
    @Transactional
    public void processPushEventAsync(
            String deliveryId,
            Map<String, Object> payload,
            Project project
    ) {
        String eventSummary = "delivery=" + deliveryId + " project=" + project.getName();
        logger.info("[Webhook] Processing push event: {}", eventSummary);

        try {
            // Extract push event fields
            String ref = (String) payload.get("ref");
            String pushedBranch = extractBranchFromRef(ref);
            String afterCommitSha = (String) payload.get("after");

            // Extract repo ID for cross-referencing
            Map<String, Object> repoMap = extractRepoMap(payload);
            Long repoId = repoMap != null ? ((Number) repoMap.get("id")).longValue() : null;

            logger.info("[Webhook] Push to branch '{}' (commit: {}) for project '{}'",
                    pushedBranch, safeShortSha(afterCommitSha), project.getName());

            // Idempotency check: reject duplicate deliveries
            if (deliveryRepository.existsByProjectIdAndDeliveryId(project.getId(), deliveryId)) {
                logger.info("[Webhook] Duplicate delivery {} — already processed, skipping", deliveryId);
                recordDelivery(project, deliveryId, EVENT_PUSH, afterCommitSha, pushedBranch,
                        GitHubWebhookDeliveryEntity.DeliveryStatus.SKIPPED, "Duplicate delivery");
                return;
            }

            // Record delivery as received
            GitHubWebhookDeliveryEntity delivery = recordDelivery(project, deliveryId, EVENT_PUSH,
                    afterCommitSha, pushedBranch, GitHubWebhookDeliveryEntity.DeliveryStatus.PROCESSING, null);

            // Auto-deploy check
            if (!project.isAutoDeploy()) {
                logger.info("[Webhook] Auto-deploy is disabled for project '{}' — skipping", project.getName());
                completeDelivery(delivery, GitHubWebhookDeliveryEntity.DeliveryStatus.SKIPPED, "Auto-deploy disabled");
                return;
            }

            // Branch match check
            String autoDeployBranch = project.getAutoDeployBranch();
            if (autoDeployBranch == null || autoDeployBranch.isBlank()) {
                autoDeployBranch = project.getGithubBranch(); // Fall back to project's configured branch
            }

            if (pushedBranch == null || !pushedBranch.equals(autoDeployBranch)) {
                logger.info("[Webhook] Push to branch '{}' does not match auto-deploy branch '{}' for project '{}' — skipping",
                        pushedBranch, autoDeployBranch, project.getName());
                completeDelivery(delivery, GitHubWebhookDeliveryEntity.DeliveryStatus.SKIPPED,
                        "Branch '" + pushedBranch + "' does not match auto-deploy branch '" + autoDeployBranch + "'");
                return;
            }

            // Handle deleted branches (afterCommitSha = 40 zeros)
            if (isDeleteEvent(afterCommitSha)) {
                logger.info("[Webhook] Branch '{}' was deleted — skipping deployment", pushedBranch);
                completeDelivery(delivery, GitHubWebhookDeliveryEntity.DeliveryStatus.SKIPPED, "Branch deletion event");
                return;
            }

            // All checks passed — trigger the full build+deploy pipeline
            logger.info("[Webhook] Triggering build+deploy for project '{}' from commit {}",
                    project.getName(), safeShortSha(afterCommitSha));

            try {
                buildDeployService.triggerBuildAndDeploy(
                        project,
                        afterCommitSha,
                        "webhook:" + deliveryId,
                        DevUserInitializer.DEV_USER_EMAIL
                );
                completeDelivery(delivery, GitHubWebhookDeliveryEntity.DeliveryStatus.COMPLETED, null);
                logger.info("[Webhook] Pipeline completed successfully for delivery {}", deliveryId);

            } catch (Exception e) {
                logger.error("[Webhook] Pipeline failed for delivery {}: {}", deliveryId, e.getMessage(), e);
                completeDelivery(delivery, GitHubWebhookDeliveryEntity.DeliveryStatus.FAILED, e.getMessage());
            }

        } catch (Exception e) {
            logger.error("[Webhook] Unexpected error processing push event {}: {}", deliveryId, e.getMessage(), e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Extract branch name from a GitHub ref string like "refs/heads/main"
     */
    public String extractBranchFromRef(String ref) {
        if (ref == null) return null;
        if (ref.startsWith(REFS_HEADS_PREFIX)) {
            return ref.substring(REFS_HEADS_PREFIX.length());
        }
        return ref;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRepoMap(Map<String, Object> payload) {
        Object repo = payload.get("repository");
        return (repo instanceof Map) ? (Map<String, Object>) repo : null;
    }

    private boolean isDeleteEvent(String afterSha) {
        return afterSha == null || afterSha.matches("^0{40}$");
    }

    private String safeShortSha(String sha) {
        if (sha == null || sha.length() < 7) return sha;
        return sha.substring(0, 7);
    }

    private GitHubWebhookDeliveryEntity recordDelivery(
            Project project, String deliveryId, String eventType,
            String commitSha, String branch,
            GitHubWebhookDeliveryEntity.DeliveryStatus status, String errorMessage
    ) {
        GitHubWebhookDeliveryEntity entity = new GitHubWebhookDeliveryEntity(project, deliveryId, eventType, commitSha, branch);
        entity.setStatus(status);
        if (errorMessage != null) {
            entity.setErrorMessage(errorMessage);
        }
        if (status == GitHubWebhookDeliveryEntity.DeliveryStatus.COMPLETED
                || status == GitHubWebhookDeliveryEntity.DeliveryStatus.SKIPPED
                || status == GitHubWebhookDeliveryEntity.DeliveryStatus.FAILED) {
            entity.setProcessedAt(ZonedDateTime.now());
        }
        return deliveryRepository.save(entity);
    }

    private void completeDelivery(
            GitHubWebhookDeliveryEntity delivery,
            GitHubWebhookDeliveryEntity.DeliveryStatus status,
            String errorMessage
    ) {
        delivery.setStatus(status);
        delivery.setProcessedAt(ZonedDateTime.now());
        if (errorMessage != null) {
            delivery.setErrorMessage(errorMessage);
        }
        deliveryRepository.save(delivery);
    }
}
