package com.azhost.github.dto;

public class GitHubCommitDto {

    private String sha;
    private String message;
    private String authorName;
    private String date;

    public GitHubCommitDto() {}

    public GitHubCommitDto(String sha, String message, String authorName, String date) {
        this.sha = sha;
        this.message = message;
        this.authorName = authorName;
        this.date = date;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
