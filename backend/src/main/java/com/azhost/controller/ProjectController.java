package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.dto.CreateProjectRequest;
import com.azhost.dto.ProjectListResponseDto;
import com.azhost.dto.ProjectResponseDto;
import com.azhost.dto.UpdateProjectRequest;
import com.azhost.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponseDto response = projectService.createProject(request, DevUserInitializer.DEV_USER_EMAIL);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ProjectListResponseDto> getProjects(@RequestParam(required = false) String search) {
        ProjectListResponseDto response = projectService.getProjects(DevUserInitializer.DEV_USER_EMAIL, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getProjectCount() {
        long count = projectService.getProjectCount(DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> getProjectById(@PathVariable UUID id) {
        ProjectResponseDto response = projectService.getProjectById(id, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> updateProject(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponseDto response = projectService.updateProject(id, request, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id, DevUserInitializer.DEV_USER_EMAIL);
        return ResponseEntity.noContent().build();
    }
}
