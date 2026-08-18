package com.azhost.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateDeploymentRequest {

    @NotNull(message = "buildId must not be null")
    private UUID buildId;

    public CreateDeploymentRequest() {}

    public CreateDeploymentRequest(UUID buildId) {
        this.buildId = buildId;
    }

    public UUID getBuildId() {
        return buildId;
    }

    public void setBuildId(UUID buildId) {
        this.buildId = buildId;
    }
}
