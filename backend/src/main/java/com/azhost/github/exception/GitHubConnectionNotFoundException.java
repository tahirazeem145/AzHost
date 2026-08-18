package com.azhost.github.exception;

public class GitHubConnectionNotFoundException extends RuntimeException {
    private final String errorCode;

    public GitHubConnectionNotFoundException(String message) {
        super(message);
        this.errorCode = "GITHUB_NOT_CONNECTED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
