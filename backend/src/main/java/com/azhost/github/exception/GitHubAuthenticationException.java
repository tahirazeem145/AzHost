package com.azhost.github.exception;

public class GitHubAuthenticationException extends RuntimeException {
    private final String errorCode;

    public GitHubAuthenticationException(String message) {
        super(message);
        this.errorCode = "GITHUB_AUTHENTICATION_FAILED";
    }

    public GitHubAuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GITHUB_AUTHENTICATION_FAILED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
