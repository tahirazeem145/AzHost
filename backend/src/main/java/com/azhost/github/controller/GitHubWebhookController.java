package com.azhost.github.controller;

import com.azhost.entity.Project;
import com.azhost.github.GitHubWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Receives GitHub webhook events.
 *
 * Endpoint: POST /api/webhooks/github
 *
 * Processing order (all synchronous before returning):
 * 1. Read raw body bytes (required for HMAC-SHA256 verification)
 * 2. Validate X-Hub-Signature-256 header
 * 3. Parse payload JSON
 * 4. Check event type (only 'push' events supported)
 * 5. Identify the target AZHost project
 * 6. Return 202 Accepted
 * 7. Process asynchronously via GitHubWebhookService
 *
 * HTTP response codes:
 * - 202 Accepted: valid webhook, processing queued
 * - 400 Bad Request: missing/malformed payload or unsupported event type
 * - 401 Unauthorized: missing or invalid signature
 * - 422 Unprocessable: valid signature but no matching project found
 */
@RestController
@RequestMapping("/api/webhooks")
public class GitHubWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookController.class);

    private static final String EVENT_PUSH = "push";
    private static final String EVENT_PING = "ping";

    private final GitHubWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final com.azhost.service.MetricsService metricsService;

    @Value("${azhost.github.webhook-secret:}")
    private String globalWebhookSecret;

    public GitHubWebhookController(
            GitHubWebhookService webhookService,
            ObjectMapper objectMapper,
            com.azhost.service.MetricsService metricsService
    ) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    /**
     * GitHub webhook receiver.
     *
     * NOTE: @RequestBody is byte[] to preserve the exact bytes for HMAC verification.
     * If we let Spring deserialize first, we may lose exact byte ordering.
     */
    @PostMapping("/github")
    public ResponseEntity<Map<String, String>> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody byte[] rawPayload
    ) {
        String safeDeliveryId = deliveryId != null ? deliveryId : "unknown";
        logger.info("[Webhook] Received GitHub event: type={} delivery={}", eventType, safeDeliveryId);
        metricsService.incrementWebhooksReceived();

        // Step 1: Validate signature is present
        if (signatureHeader == null || signatureHeader.isBlank()) {
            logger.warn("[Webhook] Rejected: missing X-Hub-Signature-256 header (delivery={})", safeDeliveryId);
            metricsService.incrementWebhooksRejected();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing webhook signature"));
        }

        // Step 2: Validate payload is not empty
        if (rawPayload == null || rawPayload.length == 0) {
            logger.warn("[Webhook] Rejected: empty payload (delivery={})", safeDeliveryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Empty webhook payload"));
        }

        // Step 3: Parse payload
        Map<String, Object> payload;
        try {
            //noinspection unchecked
            payload = objectMapper.readValue(rawPayload, Map.class);
        } catch (Exception e) {
            logger.warn("[Webhook] Rejected: malformed JSON payload (delivery={})", safeDeliveryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Malformed webhook payload"));
        }

        // Step 4: Handle ping events (GitHub sends these when you first configure a webhook)
        if (EVENT_PING.equals(eventType)) {
            logger.info("[Webhook] Received ping event — webhook endpoint is operational (delivery={})", safeDeliveryId);
            return ResponseEntity.accepted().body(Map.of("message", "Pong. AZHost webhook endpoint is active."));
        }

        // Step 5: Only process push events
        if (!EVENT_PUSH.equals(eventType)) {
            logger.info("[Webhook] Ignoring unsupported event type '{}' (delivery={})", eventType, safeDeliveryId);
            return ResponseEntity.accepted().body(Map.of("message", "Event type ignored: " + eventType));
        }

        // Step 6: Identify target project from repository ID in payload
        Long repositoryId = extractRepositoryId(payload);
        if (repositoryId == null) {
            logger.warn("[Webhook] Push payload missing repository.id field (delivery={})", safeDeliveryId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Missing repository identification in payload"));
        }

        Optional<Project> projectOpt = webhookService.findProjectByRepositoryId(repositoryId);

        // Step 7: Verify signature
        // For identified projects: verify against per-project encrypted secret
        // For unidentified projects: optionally verify against global secret before rejecting
        boolean signatureValid;
        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            signatureValid = webhookService.validateSignature(signatureHeader, rawPayload, project);
        } else {
            // Validate against global secret if configured (prevents enumeration)
            signatureValid = webhookService.validateGlobalSignature(signatureHeader, rawPayload, globalWebhookSecret);
            if (!signatureValid) {
                logger.warn("[Webhook] Rejected: invalid signature and no matching project (repo={}, delivery={})",
                        repositoryId, safeDeliveryId);
                metricsService.incrementWebhooksRejected();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid webhook signature"));
            }
            // Signature valid but no project
            logger.info("[Webhook] No AZHost project linked to repository ID {} (delivery={})", repositoryId, safeDeliveryId);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("message", "No AZHost project found for repository ID: " + repositoryId));
        }

        if (!signatureValid) {
            logger.warn("[Webhook] Rejected: invalid signature for project '{}' (delivery={})",
                    projectOpt.get().getName(), safeDeliveryId);
            metricsService.incrementWebhooksRejected();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature"));
        }

        // Step 8: Return 202 immediately — processing happens asynchronously
        Project project = projectOpt.get();
        logger.info("[Webhook] Accepted push event for project '{}' (delivery={})", project.getName(), safeDeliveryId);

        webhookService.processPushEventAsync(safeDeliveryId, payload, project);

        return ResponseEntity.accepted()
                .body(Map.of("message", "Webhook accepted", "project", project.getName(), "delivery", safeDeliveryId));
    }

    @SuppressWarnings("unchecked")
    private Long extractRepositoryId(Map<String, Object> payload) {
        try {
            Object repo = payload.get("repository");
            if (repo instanceof Map repoMap) {
                Object idObj = repoMap.get("id");
                if (idObj instanceof Number) {
                    return ((Number) idObj).longValue();
                }
            }
        } catch (Exception e) {
            logger.warn("[Webhook] Failed to extract repository ID from payload: {}", e.getMessage());
        }
        return null;
    }
}
