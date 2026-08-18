package com.azhost.exception;

public class ProjectSourceNotAvailableException extends RuntimeException {

    private final String errorCode;

    public ProjectSourceNotAvailableException(String message) {
        super(message);
        this.errorCode = "PROJECT_SOURCE_NOT_AVAILABLE";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
