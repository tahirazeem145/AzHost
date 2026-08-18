package com.azhost.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitHubRepositoryDto {

    private Long id;
    private String name;
    private String fullName;

    @JsonProperty("private")
    private boolean isPrivate;

    private String defaultBranch;
    private String htmlUrl;
    private String updatedAt;

    public GitHubRepositoryDto() {}

    public GitHubRepositoryDto(Long id, String name, String fullName, boolean isPrivate, String defaultBranch, String htmlUrl, String updatedAt) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
        this.defaultBranch = defaultBranch;
        this.htmlUrl = htmlUrl;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
