package com.azhost.github.entity;

import com.azhost.entity.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Tracks GitHub webhook deliveries for idempotency.
 * A unique constraint on (project_id, delivery_id) prevents processing
 * the same webhook delivery more than once, even if GitHub retries.
 */
@Entity
@Table(
    name = "github_webhook_deliveries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_webhook_delivery", columnNames = {"project_id", "delivery_id"})
    }
)
public class GitHubWebhookDeliveryEntity {

    public enum DeliveryStatus {
        RECEIVED,
        PROCESSING,
        COMPLETED,
        SKIPPED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank
    @Column(name = "delivery_id", nullable = false, length = 255)
    private String deliveryId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "branch", length = 255)
    private String branch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeliveryStatus status = DeliveryStatus.RECEIVED;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private ZonedDateTime receivedAt;

    @Column(name = "processed_at")
    private ZonedDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public GitHubWebhookDeliveryEntity() {}

    public GitHubWebhookDeliveryEntity(Project project, String deliveryId, String eventType, String commitSha, String branch) {
        this.project = project;
        this.deliveryId = deliveryId;
        this.eventType = eventType;
        this.commitSha = commitSha;
        this.branch = branch;
        this.status = DeliveryStatus.RECEIVED;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getDeliveryId() { return deliveryId; }
    public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public ZonedDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(ZonedDateTime receivedAt) { this.receivedAt = receivedAt; }

    public ZonedDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(ZonedDateTime processedAt) { this.processedAt = processedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
