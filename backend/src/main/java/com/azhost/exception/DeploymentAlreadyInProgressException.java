package com.azhost.exception;

public class DeploymentAlreadyInProgressException extends RuntimeException {

    private final String errorCode = "DEPLOYMENT_ALREADY_IN_PROGRESS";

    public DeploymentAlreadyInProgressException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return errorCode;
    }
}
