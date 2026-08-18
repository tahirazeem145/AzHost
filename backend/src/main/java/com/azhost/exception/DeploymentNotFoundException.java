package com.azhost.exception;

public class DeploymentNotFoundException extends RuntimeException {

    public DeploymentNotFoundException(String message) {
        super(message);
    }
}
