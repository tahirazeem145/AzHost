package com.azhost.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LinkGitHubRequestDto {

    @NotNull(message = "Repository ID is required")
    private Long repositoryId;

    @NotBlank(message = "Branch is required")
    private String branch;

    public LinkGitHubRequestDto() {}

    public LinkGitHubRequestDto(Long repositoryId, String branch) {
        this.repositoryId = repositoryId;
        this.branch = branch;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
