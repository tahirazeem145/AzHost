package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.BuildLogResponseDto;
import com.azhost.dto.BuildResponseDto;
import com.azhost.service.BuildService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
    public ResponseEntity<BuildResponseDto> startBuild(@PathVariable UUID projectId, Principal principal) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        BuildResponseDto response = buildService.startBuild(projectId, email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BuildResponseDto>> getBuilds(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        int limitSize = Math.min(size, 100);
        List<BuildResponseDto> response = buildService.getBuildsForProject(projectId, email, page, limitSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<BuildResponseDto> getBuildById(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        BuildResponseDto response = buildService.getBuildById(projectId, buildId, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{buildId}/logs")
    public ResponseEntity<BuildLogResponseDto> getBuildLogs(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        int limitSize = Math.min(size, 500);
        BuildLogResponseDto response = buildService.getBuildLogs(projectId, buildId, email, page, limitSize);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{buildId}/cancel")
    public ResponseEntity<BuildResponseDto> cancelBuild(
            @PathVariable UUID projectId,
            @PathVariable UUID buildId,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        BuildResponseDto response = buildService.cancelBuild(projectId, buildId, email);
        return ResponseEntity.ok(response);
    }
}
