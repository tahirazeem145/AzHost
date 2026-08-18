package com.azhost.github.dto;

import com.azhost.github.entity.GitHubConnectionEntity;

public class GitHubConnectionResponseDto {

    private boolean connected;
    private String githubUsername;
    private String avatarUrl;
    private String connectedAt;

    public GitHubConnectionResponseDto() {
        this.connected = false;
    }

    public GitHubConnectionResponseDto(GitHubConnectionEntity entity) {
        this.connected = true;
        this.githubUsername = entity.getGithubUsername();
        this.avatarUrl = entity.getAvatarUrl();
        this.connectedAt = entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null;
    }

    public static GitHubConnectionResponseDto disconnected() {
        return new GitHubConnectionResponseDto();
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getConnectedAt() {
        return connectedAt;
    }

    public void setConnectedAt(String connectedAt) {
        this.connectedAt = connectedAt;
    }
}
