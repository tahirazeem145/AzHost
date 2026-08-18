package com.azhost.exception;

public class BuildAlreadyInProgressException extends RuntimeException {

    private final String errorCode = "BUILD_ALREADY_IN_PROGRESS";

    public BuildAlreadyInProgressException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
