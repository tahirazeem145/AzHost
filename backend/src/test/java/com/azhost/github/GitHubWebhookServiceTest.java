package com.azhost.github;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.github.entity.GitHubWebhookDeliveryEntity;
import com.azhost.github.repository.GitHubWebhookDeliveryRepository;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.github.security.GitHubWebhookSignatureVerifier;
import com.azhost.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubWebhookServiceTest {

    private GitHubWebhookSignatureVerifier signatureVerifier;
    private GitHubTokenEncryptor tokenEncryptor;
    private ProjectRepository projectRepository;
    private GitHubWebhookDeliveryRepository deliveryRepository;
    private GitHubBuildDeployService buildDeployService;
    private GitHubWebhookService webhookService;

    private User testUser;
    private Project project;
    private static final String ENCRYPTED_SECRET = "encrypted-secret";
    private static final String PLAIN_SECRET = "test-webhook-secret";
    private static final String DELIVERY_ID = "delivery-uuid-123";

    @BeforeEach
    void setUp() {
        signatureVerifier = mock(GitHubWebhookSignatureVerifier.class);
        tokenEncryptor = mock(GitHubTokenEncryptor.class);
        projectRepository = mock(ProjectRepository.class);
        deliveryRepository = mock(GitHubWebhookDeliveryRepository.class);
        buildDeployService = mock(GitHubBuildDeployService.class);

        webhookService = new GitHubWebhookService(
                signatureVerifier, tokenEncryptor, projectRepository, deliveryRepository, buildDeployService
        );

        testUser = new User("developer@azhost.dev", "hash", "Dev");
        testUser.setId(UUID.randomUUID());

        project = new Project(testUser, "TripNest", "tripnest", "Desc",
                ProjectFramework.REACT, ProjectSourceType.GITHUB, "https://github.com/user/TripNest", "main");
        project.setId(UUID.randomUUID());
        project.setGithubRepositoryId(12345L);
        project.setGithubBranch("main");
        project.setAutoDeploy(true);
        project.setAutoDeployBranch("main");
        project.setEncryptedWebhookSecret(ENCRYPTED_SECRET);

        when(tokenEncryptor.decrypt(ENCRYPTED_SECRET)).thenReturn(PLAIN_SECRET);
        when(signatureVerifier.verify(any(), any(), eq(PLAIN_SECRET))).thenReturn(true);
        when(deliveryRepository.existsByProjectIdAndDeliveryId(any(), any())).thenReturn(false);
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Signature Validation Tests ─────────────────────────────────────────────

    @Test
    void shouldValidateSignatureSuccessfullyForConnectedProject() {
        byte[] payload = "payload-bytes".getBytes();

        boolean result = webhookService.validateSignature("sha256=abc", payload, project);

        assertTrue(result);
        verify(tokenEncryptor).decrypt(ENCRYPTED_SECRET);
        verify(signatureVerifier).verify("sha256=abc", payload, PLAIN_SECRET);
    }

    @Test
    void shouldRejectSignatureWhenProjectHasNoSecret() {
        project.setEncryptedWebhookSecret(null);
        byte[] payload = "payload-bytes".getBytes();

        assertFalse(webhookService.validateSignature("sha256=abc", payload, project));
        verifyNoInteractions(signatureVerifier);
    }

    @Test
    void shouldRejectSignatureWhenDecryptionFails() {
        when(tokenEncryptor.decrypt(ENCRYPTED_SECRET)).thenThrow(new RuntimeException("Decryption error"));
        byte[] payload = "payload-bytes".getBytes();

        assertFalse(webhookService.validateSignature("sha256=abc", payload, project));
        verifyNoInteractions(signatureVerifier);
    }

    // ── Project Lookup Tests ───────────────────────────────────────────────────

    @Test
    void shouldFindProjectByRepositoryId() {
        when(projectRepository.findAllByGithubRepositoryId(12345L)).thenReturn(List.of(project));

        Optional<Project> result = webhookService.findProjectByRepositoryId(12345L);

        assertTrue(result.isPresent());
        assertEquals(project.getName(), result.get().getName());
    }

    @Test
    void shouldReturnEmptyWhenNoProjectForRepoId() {
        when(projectRepository.findAllByGithubRepositoryId(99999L)).thenReturn(List.of());

        Optional<Project> result = webhookService.findProjectByRepositoryId(99999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullRepositoryId() {
        Optional<Project> result = webhookService.findProjectByRepositoryId(null);

        assertTrue(result.isEmpty());
        verifyNoInteractions(projectRepository);
    }

    // ── Push Event Processing Tests ────────────────────────────────────────────

    @Test
    void shouldSkipDuplicateDeliveries() {
        when(deliveryRepository.existsByProjectIdAndDeliveryId(project.getId(), DELIVERY_ID)).thenReturn(true);

        Map<String, Object> payload = buildPushPayload("refs/heads/main", "abc123commit");
        webhookService.processPushEventAsync(DELIVERY_ID, payload, project);

        verifyNoInteractions(buildDeployService);
    }

    @Test
    void shouldSkipWhenAutoDeployIsDisabled() {
        project.setAutoDeploy(false);

        Map<String, Object> payload = buildPushPayload("refs/heads/main", "abc123commit");
        webhookService.processPushEventAsync(DELIVERY_ID, payload, project);

        verifyNoInteractions(buildDeployService);
    }

    @Test
    void shouldSkipWhenBranchDoesNotMatchAutoDeployBranch() {
        // Pushing to 'feature-xyz' but auto-deploy is configured for 'main'
        Map<String, Object> payload = buildPushPayload("refs/heads/feature-xyz", "abc123commit");
        webhookService.processPushEventAsync(DELIVERY_ID, payload, project);

        verifyNoInteractions(buildDeployService);
    }

    @Test
    void shouldSkipBranchDeletionEvents() {
        // GitHub sends all zeros for "after" when a branch is deleted
        Map<String, Object> payload = buildPushPayload("refs/heads/main", "0000000000000000000000000000000000000000");
        webhookService.processPushEventAsync(DELIVERY_ID, payload, project);

        verifyNoInteractions(buildDeployService);
    }

    @Test
    void shouldTriggerBuildAndDeployForValidPushEvent() {
        doNothing().when(buildDeployService).triggerBuildAndDeploy(any(), anyString(), anyString(), anyString());

        Map<String, Object> payload = buildPushPayload("refs/heads/main", "abc123def456");
        webhookService.processPushEventAsync(DELIVERY_ID, payload, project);

        verify(buildDeployService).triggerBuildAndDeploy(
                eq(project),
                eq("abc123def456"),
                contains(DELIVERY_ID),
                anyString()
        );
    }

    @Test
    void shouldRecordFailedDeliveryWhenPipelineFails() {
        doThrow(new RuntimeException("Build failed"))
                .when(buildDeployService).triggerBuildAndDeploy(any(), any(), any(), any());

        Map<String, Object> payload = buildPushPayload("refs/heads/main", "abc123def456");
        webhookService.processPushEventAsync(DELIVERY_ID, payload, project);

        // Delivery should be saved (multiple times: first as PROCESSING, then as FAILED)
        verify(deliveryRepository, atLeastOnce()).save(argThat(delivery ->
                delivery.getStatus() == GitHubWebhookDeliveryEntity.DeliveryStatus.FAILED
                        || delivery.getStatus() == GitHubWebhookDeliveryEntity.DeliveryStatus.PROCESSING
        ));
    }

    // ── Ref Parsing Tests ──────────────────────────────────────────────────────

    @Test
    void shouldExtractBranchFromRef() {
        assertEquals("main", webhookService.extractBranchFromRef("refs/heads/main"));
        assertEquals("feature/my-feature", webhookService.extractBranchFromRef("refs/heads/feature/my-feature"));
        assertEquals("develop", webhookService.extractBranchFromRef("refs/heads/develop"));
    }

    @Test
    void shouldHandleNullOrUnexpectedRef() {
        assertNull(webhookService.extractBranchFromRef(null));
        // Non-standard ref (tags) returned as-is
        assertEquals("refs/tags/v1.0", webhookService.extractBranchFromRef("refs/tags/v1.0"));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Map<String, Object> buildPushPayload(String ref, String afterCommitSha) {
        return Map.of(
                "ref", ref,
                "after", afterCommitSha,
                "repository", Map.of(
                        "id", 12345L,
                        "name", "TripNest",
                        "full_name", "user/TripNest"
                )
        );
    }
}
