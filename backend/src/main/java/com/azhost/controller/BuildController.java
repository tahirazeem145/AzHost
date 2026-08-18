package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.BuildLogResponseDto;
import com.azhost.dto.BuildResponseDto;
import com.azhost.service.BuildService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/builds")
public class BuildController {

    private final BuildService buildService;

    public BuildController(BuildService buildService) {
        this.buildService = buildService;
    }

    @PostMapping
    public ResponseEntity<BuildResponseDto> startBuild(@PathVariable UUID projectId) {
        BuildResponseDto response = buildService.startBuild(projectId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BuildResponseDto>> getBuilds(@PathVariable UUID projectId) {
        List<BuildResponseDto> response = buildService.getBuildsForProject(projectId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<BuildResponseDto> getBuildById(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId
    ) {
        BuildResponseDto response = buildService.getBuildById(projectId, buildId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{buildId}/logs")
    public ResponseEntity<BuildLogResponseDto> getBuildLogs(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId
    ) {
        BuildLogResponseDto response = buildService.getBuildLogs(projectId, buildId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{buildId}/cancel")
    public ResponseEntity<BuildResponseDto> cancelBuild(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId
    ) {
        BuildResponseDto response = buildService.cancelBuild(projectId, buildId, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }
}
