package com.azhost.dto;

import com.azhost.build.BuildStatus;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.entity.ProjectFramework;

import java.time.ZonedDateTime;
import java.util.UUID;

public class BuildResponseDto {

    private UUID id;
    private UUID projectId;
    private BuildStatus status;
    private ProjectFramework framework;
    private String packageManager;
    private String nodeVersion;
    private String buildCommand;
    private String outputDirectory;
    private String artifactId;
    private ZonedDateTime startedAt;
    private ZonedDateTime completedAt;
    private Long durationMs;
    private Integer exitCode;
    private String errorMessage;
    private ZonedDateTime createdAt;

    public BuildResponseDto() {}

    public BuildResponseDto(ProjectBuildEntity entity) {
        this.id = entity.getId();
        this.projectId = entity.getProject().getId();
        this.status = entity.getStatus();
        this.framework = entity.getFramework();
        this.packageManager = entity.getPackageManager();
        this.nodeVersion = entity.getNodeVersion();
        this.buildCommand = entity.getBuildCommand();
        this.outputDirectory = entity.getOutputDirectory();
        this.artifactId = entity.getArtifactId();
        this.startedAt = entity.getStartedAt();
        this.completedAt = entity.getCompletedAt();
        this.durationMs = entity.getDurationMs();
        this.exitCode = entity.getExitCode();
        this.errorMessage = entity.getErrorMessage();
        this.createdAt = entity.getCreatedAt();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public BuildStatus getStatus() { return status; }
    public void setStatus(BuildStatus status) { this.status = status; }

    public ProjectFramework getFramework() { return framework; }
    public void setFramework(ProjectFramework framework) { this.framework = framework; }

    public String getPackageManager() { return packageManager; }
    public void setPackageManager(String packageManager) { this.packageManager = packageManager; }

    public String getNodeVersion() { return nodeVersion; }
    public void setNodeVersion(String nodeVersion) { this.nodeVersion = nodeVersion; }

    public String getBuildCommand() { return buildCommand; }
    public void setBuildCommand(String buildCommand) { this.buildCommand = buildCommand; }

    public String getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }

    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public ZonedDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(ZonedDateTime startedAt) { this.startedAt = startedAt; }

    public ZonedDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(ZonedDateTime completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
