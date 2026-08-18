package com.azhost.github.exception;

public class GitHubAuthorizationException extends RuntimeException {
    private final String errorCode;

    public GitHubAuthorizationException(String message) {
        super(message);
        this.errorCode = "GITHUB_ACCESS_DENIED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
