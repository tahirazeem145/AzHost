package com.azhost.dto;

import java.util.List;

public class DeploymentListResponseDto {

    private List<DeploymentResponseDto> deployments;

    public DeploymentListResponseDto() {}

    public DeploymentListResponseDto(List<DeploymentResponseDto> deployments) {
        this.deployments = deployments;
    }

    public List<DeploymentResponseDto> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<DeploymentResponseDto> deployments) {
        this.deployments = deployments;
    }
}
