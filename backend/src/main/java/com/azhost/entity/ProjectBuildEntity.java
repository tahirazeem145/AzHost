package com.azhost.entity;

import com.azhost.build.BuildStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_builds")
public class ProjectBuildEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BuildStatus status = BuildStatus.QUEUED;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectFramework framework;

    @NotBlank
    @Column(name = "package_manager", nullable = false, length = 50)
    private String packageManager;

    @NotBlank
    @Column(name = "node_version", nullable = false, length = 50)
    private String nodeVersion;

    @Column(name = "build_command")
    private String buildCommand;

    @NotBlank
    @Column(name = "output_directory", nullable = false, length = 100)
    private String outputDirectory;

    @NotBlank
    @Column(name = "workspace_id", nullable = false, length = 100)
    private String workspaceId;

    @Column(name = "artifact_id", length = 100)
    private String artifactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50)
    private ProjectSourceType sourceType;

    @Column(name = "github_repository_id")
    private Long githubRepositoryId;

    @Column(name = "github_commit_sha", length = 64)
    private String githubCommitSha;

    @Column(name = "docker_container_id")
    private String dockerContainerId;

    @Column(name = "started_at")
    private ZonedDateTime startedAt;


    @Column(name = "claimed_by")
    private String claimedBy;

    @Column(name = "claimed_at")
    private ZonedDateTime claimedAt;

    @Column(name = "heartbeat_at")
    private ZonedDateTime heartbeatAt;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public ProjectBuildEntity() {}

    public ProjectBuildEntity(Project project, ProjectFramework framework, String packageManager, String nodeVersion, String buildCommand, String outputDirectory, String workspaceId) {
        this.project = project;
        this.framework = framework;
        this.packageManager = packageManager;
        this.nodeVersion = nodeVersion;
        this.buildCommand = buildCommand;
        this.outputDirectory = outputDirectory;
        this.workspaceId = workspaceId;
        this.status = BuildStatus.QUEUED;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public BuildStatus getStatus() {
        return status;
    }

    public void setStatus(BuildStatus status) {
        this.status = status;
    }

    public ProjectFramework getFramework() {
        return framework;
    }

    public void setFramework(ProjectFramework framework) {
        this.framework = framework;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public void setPackageManager(String packageManager) {
        this.packageManager = packageManager;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public void setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public ZonedDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(ZonedDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ProjectSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ProjectSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Long getGithubRepositoryId() {
        return githubRepositoryId;
    }

    public void setGithubRepositoryId(Long githubRepositoryId) {
        this.githubRepositoryId = githubRepositoryId;
    }

    public String getGithubCommitSha() {
        return githubCommitSha;
    }

    public void setGithubCommitSha(String githubCommitSha) {
        this.githubCommitSha = githubCommitSha;
    }

    public String getDockerContainerId() {
        return dockerContainerId;
    }

    public void setDockerContainerId(String dockerContainerId) {
        this.dockerContainerId = dockerContainerId;
    }

    public String getClaimedBy() {
        return claimedBy;
    }

    public void setClaimedBy(String claimedBy) {
        this.claimedBy = claimedBy;
    }

    public ZonedDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(ZonedDateTime claimedAt) {
        this.claimedAt = claimedAt;
    }

    public ZonedDateTime getHeartbeatAt() {
        return heartbeatAt;
    }

    public void setHeartbeatAt(ZonedDateTime heartbeatAt) {
        this.heartbeatAt = heartbeatAt;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

