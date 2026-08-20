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

import java.security.Principal;
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
    public ResponseEntity<ProjectResponseDto> createProject(@Valid @RequestBody CreateProjectRequest request, Principal principal) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        ProjectResponseDto response = projectService.createProject(request, email);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ProjectListResponseDto> getProjects(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        if (page == null && size == null) {
            ProjectListResponseDto response = projectService.getProjects(email, search);
            return ResponseEntity.ok(response);
        }
        int p = page != null ? page : 0;
        int s = size != null ? size : 10;
        int limitSize = Math.min(s, 100);
        ProjectListResponseDto response = projectService.getProjects(email, search, p, limitSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getProjectCount(Principal principal) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        long count = projectService.getProjectCount(email);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> getProjectById(@PathVariable UUID id, Principal principal) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        ProjectResponseDto response = projectService.getProjectById(id, email);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        ProjectResponseDto response = projectService.updateProject(id, request, email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id, Principal principal) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        projectService.deleteProject(id, email);
        return ResponseEntity.noContent().build();
    }
}
