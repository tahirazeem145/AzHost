package com.azhost.exception;

public class BuildEngineUnavailableException extends RuntimeException {

    private final String errorCode = "BUILD_ENGINE_UNAVAILABLE";

    public BuildEngineUnavailableException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
