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

import java.security.Principal;
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
            @Valid @RequestBody CreateDeploymentRequest request,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        DeploymentResponseDto response = deploymentService.createDeployment(projectId, request, email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<DeploymentListResponseDto> getDeployments(
            @PathVariable UUID projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        if (page == null && size == null) {
            DeploymentListResponseDto response = deploymentService.getDeploymentsForProject(projectId, email);
            return ResponseEntity.ok(response);
        }
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        int limitSize = Math.min(s, 100);
        DeploymentListResponseDto response = deploymentService.getDeploymentsForProject(projectId, email, p, limitSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{deploymentId}")
    public ResponseEntity<DeploymentResponseDto> getDeploymentById(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        DeploymentResponseDto response = deploymentService.getDeploymentById(projectId, deploymentId, email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{deploymentId}/cancel")
    public ResponseEntity<DeploymentResponseDto> cancelDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        DeploymentResponseDto response = deploymentService.cancelDeployment(projectId, deploymentId, email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{deploymentId}/rollback")
    public ResponseEntity<DeploymentResponseDto> rollbackToDeployment(
            @PathVariable UUID projectId,
            @PathVariable UUID deploymentId,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        DeploymentResponseDto response = deploymentService.rollbackToDeployment(projectId, deploymentId, email);
        return ResponseEntity.ok(response);
    }
}
