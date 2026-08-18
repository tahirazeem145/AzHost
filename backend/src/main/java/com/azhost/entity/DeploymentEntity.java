package com.azhost.entity;

import com.azhost.deployment.DeploymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployments")
public class DeploymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "build_id", nullable = false)
    private ProjectBuildEntity build;

    @NotBlank
    @Column(name = "artifact_id", nullable = false, length = 100)
    private String artifactId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DeploymentStatus status = DeploymentStatus.QUEUED;

    @Column(name = "deployment_path")
    private String deploymentPath;

    @Column(name = "deployment_url", length = 500)
    private String deploymentUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "published_at")
    private ZonedDateTime publishedAt;

    @Column(name = "failed_at")
    private ZonedDateTime failedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public DeploymentEntity() {}

    public DeploymentEntity(Project project, ProjectBuildEntity build, String artifactId) {
        this.project = project;
        this.build = build;
        this.artifactId = artifactId;
        this.status = DeploymentStatus.QUEUED;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public ProjectBuildEntity getBuild() { return build; }
    public void setBuild(ProjectBuildEntity build) { this.build = build; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public DeploymentStatus getStatus() { return status; }
    public void setStatus(DeploymentStatus status) { this.status = status; }

    public String getDeploymentPath() { return deploymentPath; }
    public void setDeploymentPath(String deploymentPath) { this.deploymentPath = deploymentPath; }

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
}
