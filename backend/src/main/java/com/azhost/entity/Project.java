package com.azhost.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects", uniqueConstraints = {
    @UniqueConstraint(name = "uk_projects_user_slug", columnNames = {"user_id", "slug"})
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectFramework framework;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private ProjectSourceType sourceType;

    @Column(name = "repository_url")
    private String repositoryUrl;

    @Column(name = "repository_branch", length = 100)
    private String repositoryBranch;

    @Column(name = "github_repository_id")
    private Long githubRepositoryId;

    @Column(name = "github_repository_name")
    private String githubRepositoryName;

    @Column(name = "github_branch")
    private String githubBranch;

    @Column(name = "github_commit_sha", length = 64)
    private String githubCommitSha;

    // Auto-deploy settings
    @Column(name = "auto_deploy", nullable = false)
    private boolean autoDeploy = false;

    @Column(name = "auto_deploy_branch", length = 255)
    private String autoDeployBranch;

    // Per-project GitHub webhook secret — stored encrypted at rest
    @Column(name = "encrypted_webhook_secret", columnDefinition = "TEXT")
    private String encryptedWebhookSecret;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_deployment_id")
    private DeploymentEntity activeDeployment;

    @Column(name = "deployment_counter", nullable = false)
    private long deploymentCounter = 0L;

    @Column(name = "reserved_storage_bytes", nullable = false)
    private long reservedStorageBytes = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;


    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public Project() {}

    public Project(User user, String name, String slug, String description, ProjectFramework framework, ProjectSourceType sourceType, String repositoryUrl, String repositoryBranch) {
        this.user = user;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.framework = framework;
        this.sourceType = sourceType;
        this.repositoryUrl = repositoryUrl;
        this.repositoryBranch = repositoryBranch;
        this.status = ProjectStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProjectFramework getFramework() {
        return framework;
    }

    public void setFramework(ProjectFramework framework) {
        this.framework = framework;
    }

    public ProjectSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ProjectSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getRepositoryBranch() {
        return repositoryBranch;
    }

    public void setRepositoryBranch(String repositoryBranch) {
        this.repositoryBranch = repositoryBranch;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public DeploymentEntity getActiveDeployment() {
        return activeDeployment;
    }

    public void setActiveDeployment(DeploymentEntity activeDeployment) {
        this.activeDeployment = activeDeployment;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getGithubRepositoryId() {
        return githubRepositoryId;
    }

    public void setGithubRepositoryId(Long githubRepositoryId) {
        this.githubRepositoryId = githubRepositoryId;
    }

    public String getGithubRepositoryName() {
        return githubRepositoryName;
    }

    public void setGithubRepositoryName(String githubRepositoryName) {
        this.githubRepositoryName = githubRepositoryName;
    }

    public String getGithubBranch() {
        return githubBranch;
    }

    public void setGithubBranch(String githubBranch) {
        this.githubBranch = githubBranch;
    }

    public String getGithubCommitSha() {
        return githubCommitSha;
    }

    public void setGithubCommitSha(String githubCommitSha) {
        this.githubCommitSha = githubCommitSha;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAutoDeploy() {
        return autoDeploy;
    }

    public void setAutoDeploy(boolean autoDeploy) {
        this.autoDeploy = autoDeploy;
    }

    public String getAutoDeployBranch() {
        return autoDeployBranch;
    }

    public void setAutoDeployBranch(String autoDeployBranch) {
        this.autoDeployBranch = autoDeployBranch;
    }

    public String getEncryptedWebhookSecret() {
        return encryptedWebhookSecret;
    }

    public void setEncryptedWebhookSecret(String encryptedWebhookSecret) {
        this.encryptedWebhookSecret = encryptedWebhookSecret;
    }

    public long getDeploymentCounter() {
        return deploymentCounter;
    }

    public void setDeploymentCounter(long deploymentCounter) {
        this.deploymentCounter = deploymentCounter;
    }

    public long getReservedStorageBytes() {
        return reservedStorageBytes;
    }

    public void setReservedStorageBytes(long reservedStorageBytes) {
        this.reservedStorageBytes = reservedStorageBytes;
    }
}

