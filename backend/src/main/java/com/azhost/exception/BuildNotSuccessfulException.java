package com.azhost.exception;

public class BuildNotSuccessfulException extends RuntimeException {

    private final String errorCode = "BUILD_NOT_SUCCESSFUL";

    public BuildNotSuccessfulException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
