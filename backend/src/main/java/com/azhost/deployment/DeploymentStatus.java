package com.azhost.deployment;

public enum DeploymentStatus {
    QUEUED,
    PREPARING,
    EXTRACTING,
    VALIDATING,
    PUBLISHING,
    SUCCESS,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
