package com.azhost.deployment;

public class DeploymentResult {

    private final DeploymentStatus status;
    private final String deploymentPath;
    private final String deploymentUrl;
    private final String errorMessage;

    public DeploymentResult(DeploymentStatus status, String deploymentPath, String deploymentUrl, String errorMessage) {
        this.status = status;
        this.deploymentPath = deploymentPath;
        this.deploymentUrl = deploymentUrl;
        this.errorMessage = errorMessage;
    }

    public static DeploymentResult success(String deploymentPath, String deploymentUrl) {
        return new DeploymentResult(DeploymentStatus.SUCCESS, deploymentPath, deploymentUrl, null);
    }

    public static DeploymentResult failed(DeploymentStatus status, String errorMessage) {
        return new DeploymentResult(status, null, null, errorMessage);
    }

    public DeploymentStatus getStatus() { return status; }
    public String getDeploymentPath() { return deploymentPath; }
    public String getDeploymentUrl() { return deploymentUrl; }
    public String getErrorMessage() { return errorMessage; }
}
