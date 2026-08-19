package com.azhost.dto;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.ProjectStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

public class ProjectResponseDto {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private ProjectFramework framework;
    private ProjectSourceType sourceType;
    private String repositoryUrl;
    private String repositoryBranch;
    private Long githubRepositoryId;
    private String githubRepositoryName;
    private String githubBranch;
    private String githubCommitSha;
    private ProjectStatus status;
    private boolean autoDeploy;
    private String autoDeployBranch;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public ProjectResponseDto() {}

    public ProjectResponseDto(Project project) {
        this.id = project.getId();
        this.name = project.getName();
        this.slug = project.getSlug();
        this.description = project.getDescription();
        this.framework = project.getFramework();
        this.sourceType = project.getSourceType();
        this.repositoryUrl = project.getRepositoryUrl();
        this.repositoryBranch = project.getRepositoryBranch();
        this.githubRepositoryId = project.getGithubRepositoryId();
        this.githubRepositoryName = project.getGithubRepositoryName();
        this.githubBranch = project.getGithubBranch();
        this.githubCommitSha = project.getGithubCommitSha();
        this.status = project.getStatus();
        this.autoDeploy = project.isAutoDeploy();
        this.autoDeployBranch = project.getAutoDeployBranch();
        this.createdAt = project.getCreatedAt();
        this.updatedAt = project.getUpdatedAt();
    }


    public ProjectResponseDto(UUID id, String name, String slug, String description, ProjectFramework framework, ProjectSourceType sourceType, String repositoryUrl, String repositoryBranch, ProjectStatus status, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.framework = framework;
        this.sourceType = sourceType;
        this.repositoryUrl = repositoryUrl;
        this.repositoryBranch = repositoryBranch;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public ProjectStatus getStatus() {

        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
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
}
