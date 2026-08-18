package com.azhost.github.exception;

public class GitHubSourceAcquisitionException extends RuntimeException {
    private final String errorCode;

    public GitHubSourceAcquisitionException(String message) {
        super(message);
        this.errorCode = "GITHUB_SOURCE_DOWNLOAD_FAILED";
    }

    public GitHubSourceAcquisitionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GitHubSourceAcquisitionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GITHUB_SOURCE_DOWNLOAD_FAILED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
