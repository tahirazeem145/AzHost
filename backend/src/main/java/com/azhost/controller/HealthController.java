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

            // 2. Check build workspace directories
            if (!Files.exists(workspaceManager.getWorkspacesRoot())) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("status", "DOWN", "error", "Workspace root directory unavailable"));
            }

            return ResponseEntity.ok(Map.of("status", "UP"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "DOWN", "error", e.getMessage()));
        }
    }
}
