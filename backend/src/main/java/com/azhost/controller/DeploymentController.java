package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.CreateDeploymentRequest;
import com.azhost.dto.DeploymentListResponseDto;
import com.azhost.dto.DeploymentResponseDto;
import com.azhost.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/deployments")
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    public ResponseEntity<DeploymentResponseDto> createDeployment(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateDeploymentRequest request
    ) {
        DeploymentResponseDto response = deploymentService.createDeployment(projectId, request, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<DeploymentListResponseDto> getDeployments(@PathVariable UUID projectId) {
        DeploymentListResponseDto response = deploymentService.getDeploymentsForProject(projectId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{deploymentId}")
    public ResponseEntity<DeploymentResponseDto> getDeploymentById(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId
    ) {
        DeploymentResponseDto response = deploymentService.getDeploymentById(projectId, deploymentId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{deploymentId}/cancel")
    public ResponseEntity<DeploymentResponseDto> cancelDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId
    ) {
        DeploymentResponseDto response = deploymentService.cancelDeployment(projectId, deploymentId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{deploymentId}/rollback")
    public ResponseEntity<DeploymentResponseDto> rollbackToDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId
    ) {
        DeploymentResponseDto response = deploymentService.rollbackToDeployment(projectId, deploymentId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }
}
