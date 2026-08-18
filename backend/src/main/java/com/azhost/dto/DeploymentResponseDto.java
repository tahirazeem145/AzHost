package com.azhost.dto;

import com.azhost.deployment.DeploymentStatus;
import com.azhost.entity.DeploymentEntity;

import java.time.ZonedDateTime;
import java.util.UUID;

public class DeploymentResponseDto {

    private UUID id;
    private UUID projectId;
    private UUID buildId;
    private String artifactId;
    private DeploymentStatus status;
    private String deploymentUrl;
    private ZonedDateTime createdAt;
    private ZonedDateTime publishedAt;
    private ZonedDateTime failedAt;
    private String errorMessage;
    private boolean active;

    public DeploymentResponseDto() {}

    public DeploymentResponseDto(DeploymentEntity entity) {
        this(entity, false);
    }

    public DeploymentResponseDto(DeploymentEntity entity, boolean active) {
        this.id = entity.getId();
        this.projectId = entity.getProject().getId();
        this.buildId = entity.getBuild().getId();
        this.artifactId = entity.getArtifactId();
        this.status = entity.getStatus();
        this.deploymentUrl = entity.getDeploymentUrl();
        this.createdAt = entity.getCreatedAt();
        this.publishedAt = entity.getPublishedAt();
        this.failedAt = entity.getFailedAt();
        this.errorMessage = entity.getErrorMessage();
        this.active = active;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getBuildId() { return buildId; }
    public void setBuildId(UUID buildId) { this.buildId = buildId; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public DeploymentStatus getStatus() { return status; }
    public void setStatus(DeploymentStatus status) { this.status = status; }

    public String getDeploymentUrl() { return deploymentUrl; }
    public void setDeploymentUrl(String deploymentUrl) { this.deploymentUrl = deploymentUrl; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(ZonedDateTime publishedAt) { this.publishedAt = publishedAt; }

    public ZonedDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(ZonedDateTime failedAt) { this.failedAt = failedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
