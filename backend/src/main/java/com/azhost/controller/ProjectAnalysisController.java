package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.ProjectAnalysisResponseDto;
import com.azhost.service.ProjectAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectAnalysisController {

    private final ProjectAnalysisService projectAnalysisService;

    public ProjectAnalysisController(ProjectAnalysisService projectAnalysisService) {
        this.projectAnalysisService = projectAnalysisService;
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<ProjectAnalysisResponseDto> analyzeProject(@PathVariable UUID id) {
        ProjectAnalysisResponseDto response = projectAnalysisService.analyzeProject(id, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<ProjectAnalysisResponseDto> getLatestAnalysis(@PathVariable UUID id) {
        ProjectAnalysisResponseDto response = projectAnalysisService.getLatestAnalysis(id, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }
}
