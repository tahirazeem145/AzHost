package com.azhost.dto;

import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Framework selection is required")
    private ProjectFramework framework;

    @NotNull(message = "Source type is required")
    private ProjectSourceType sourceType;

    private String repositoryUrl;

    @Size(max = 100, message = "Repository branch cannot exceed 100 characters")
    private String repositoryBranch;

    public CreateProjectRequest() {}

    public CreateProjectRequest(String name, String description, ProjectFramework framework, ProjectSourceType sourceType, String repositoryUrl, String repositoryBranch) {
        this.name = name;
        this.description = description;
        this.framework = framework;
        this.sourceType = sourceType;
        this.repositoryUrl = repositoryUrl;
        this.repositoryBranch = repositoryBranch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
