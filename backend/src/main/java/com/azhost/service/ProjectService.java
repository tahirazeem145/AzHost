package com.azhost.service;

import com.azhost.dto.CreateProjectRequest;
import com.azhost.dto.ProjectListResponseDto;
import com.azhost.dto.ProjectResponseDto;
import com.azhost.dto.UpdateProjectRequest;
import com.azhost.entity.*;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final SlugService slugService;
    private final AuditLogService auditLogService;
    private final ProjectAuthorizationService projectAuthorizationService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            SlugService slugService,
            AuditLogService auditLogService,
            ProjectAuthorizationService projectAuthorizationService
    ) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.slugService = slugService;
        this.auditLogService = auditLogService;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional
    public ProjectResponseDto createProject(CreateProjectRequest request, String userEmail) {
        User user = getUser(userEmail);
        String uniqueSlug = slugService.generateUniqueSlug(user.getId(), request.getName());

        Project project = new Project(
                user,
                request.getName().trim(),
                uniqueSlug,
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getFramework(),
                request.getSourceType(),
                request.getRepositoryUrl() != null ? request.getRepositoryUrl().trim() : null,
                request.getRepositoryBranch() != null && !request.getRepositoryBranch().isBlank() ? request.getRepositoryBranch().trim() : "main"
        );

        Project savedProject = projectRepository.save(project);
        auditLogService.log(user, savedProject, "PROJECT_CREATED", "Project", savedProject.getId().toString(), "SUCCESS", "Created project " + savedProject.getName());
        logger.info("Created project '{}' with slug '{}' for user '{}'", savedProject.getName(), savedProject.getSlug(), userEmail);
        return new ProjectResponseDto(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectListResponseDto getProjects(String userEmail, String search) {
        return getProjects(userEmail, search, 0, 100);
    }

    @Transactional(readOnly = true)
    public ProjectListResponseDto getProjects(String userEmail, String search, int page, int size) {
        User user = getUser(userEmail);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Project> projectPage;

        if (search != null && !search.isBlank()) {
            projectPage = projectRepository.searchAccessibleProjects(user.getId(), search.trim(), pageable);
        } else {
            projectPage = projectRepository.findAllAccessibleProjects(user.getId(), pageable);
        }

        List<ProjectResponseDto> dtos = projectPage.getContent().stream()
                .map(ProjectResponseDto::new)
                .collect(Collectors.toList());

        return new ProjectListResponseDto(dtos, projectPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto getProjectById(UUID id, String userEmail) {
        Project project = projectAuthorizationService.verifyAccess(id, userEmail, ProjectRole.VIEWER);
        return new ProjectResponseDto(project);
    }

    @Transactional
    public ProjectResponseDto updateProject(UUID id, UpdateProjectRequest request, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectAuthorizationService.verifyAccess(id, userEmail, ProjectRole.OWNER);

        // If name changed, generate new unique slug
        if (!project.getName().equalsIgnoreCase(request.getName().trim())) {
            String newSlug = slugService.generateUniqueSlug(project.getUser().getId(), request.getName());
            project.setSlug(newSlug);
        }

        project.setName(request.getName().trim());
        project.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        project.setFramework(request.getFramework());
        project.setRepositoryUrl(request.getRepositoryUrl() != null ? request.getRepositoryUrl().trim() : null);
        project.setRepositoryBranch(request.getRepositoryBranch() != null && !request.getRepositoryBranch().isBlank() ? request.getRepositoryBranch().trim() : "main");
        project.setStatus(request.getStatus());

        // Auto-deploy settings
        if (request.getAutoDeploy() != null) {
            project.setAutoDeploy(request.getAutoDeploy());
        }
        if (request.getAutoDeployBranch() != null) {
            project.setAutoDeployBranch(request.getAutoDeployBranch().isBlank() ? null : request.getAutoDeployBranch().trim());
        }

        Project updatedProject = projectRepository.save(project);
        auditLogService.log(user, updatedProject, "PROJECT_UPDATED", "Project", updatedProject.getId().toString(), "SUCCESS", "Updated project " + updatedProject.getName());
        logger.info("Updated project '{}' (ID: {}) for user '{}'", updatedProject.getName(), id, userEmail);
        return new ProjectResponseDto(updatedProject);
    }

    @Transactional
    public void deleteProject(UUID id, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectAuthorizationService.verifyAccess(id, userEmail, ProjectRole.OWNER);
        projectRepository.delete(project);
        auditLogService.log(user, null, "PROJECT_DELETED", "Project", id.toString(), "SUCCESS", "Deleted project " + project.getName());
        logger.info("Deleted project '{}' (ID: {}) for user '{}'", project.getName(), id, userEmail);
    }

    @Transactional(readOnly = true)
    public long getProjectCount(String userEmail) {
        User user = getUser(userEmail);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 1);
        return projectRepository.findAllAccessibleProjects(user.getId(), pageable).getTotalElements();
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
