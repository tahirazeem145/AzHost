package com.azhost.controller;

import com.azhost.build.executor.BuildExecutor;
import com.azhost.build.workspace.BuildWorkspaceManager;
import com.azhost.dto.HealthResponseDto;
import com.azhost.service.SystemInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final SystemInfoService systemInfoService;
    private final JdbcTemplate jdbcTemplate;
    private final BuildWorkspaceManager workspaceManager;
    private final BuildExecutor buildExecutor;

    public HealthController(
            SystemInfoService systemInfoService,
            JdbcTemplate jdbcTemplate,
            BuildWorkspaceManager workspaceManager,
            BuildExecutor buildExecutor
    ) {
        this.systemInfoService = systemInfoService;
        this.jdbcTemplate = jdbcTemplate;
        this.workspaceManager = workspaceManager;
        this.buildExecutor = buildExecutor;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> getHealth() {
        return ResponseEntity.ok(systemInfoService.getHealthStatus());
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> getReady() {
        try {
            // 1. Check database connectivity
            jdbcTemplate.execute("SELECT 1");

            // 2. Check build workspace directories are present and writable
            Path workspaceRoot = workspaceManager.getWorkspacesRoot();
            if (!Files.exists(workspaceRoot)) {
                try {
                    Files.createDirectories(workspaceRoot);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of("status", "DOWN", "error", "Workspace root directory could not be created"));
                }
            }
            if (!Files.isWritable(workspaceRoot)) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("status", "DOWN", "error", "Workspace root directory is not writable"));
            }

            // 3. Check artifacts directory is present and writable
            Path artifactsRoot = workspaceManager.getArtifactsRoot();
            if (!Files.exists(artifactsRoot)) {
                try {
                    Files.createDirectories(artifactsRoot);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body(Map.of("status", "DOWN", "error", "Artifacts root directory could not be created"));
                }
            }
            if (!Files.isWritable(artifactsRoot)) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("status", "DOWN", "error", "Artifacts root directory is not writable"));
            }

            // 4. Check Docker daemon availability
            if (!buildExecutor.isDockerAvailable()) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("status", "DOWN", "error", "Docker daemon is unavailable"));
            }

            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "error", "Critical dependency failure"));
        }
    }
}
