package com.azhost.github.exception;

public class GitHubRepositoryNotFoundException extends RuntimeException {
    private final String errorCode;

    public GitHubRepositoryNotFoundException(String message) {
        super(message);
        this.errorCode = "GITHUB_REPOSITORY_NOT_FOUND";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
