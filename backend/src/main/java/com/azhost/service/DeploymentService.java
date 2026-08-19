package com.azhost.service;

import com.azhost.build.workspace.BuildWorkspaceManager;
import com.azhost.deployment.DeploymentManager;
import com.azhost.deployment.DeploymentStatus;
import com.azhost.deployment.DeploymentValidator;
import com.azhost.dto.CreateDeploymentRequest;
import com.azhost.dto.DeploymentListResponseDto;
import com.azhost.dto.DeploymentResponseDto;
import com.azhost.entity.DeploymentEntity;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.entity.User;
import com.azhost.exception.BuildNotFoundException;
import com.azhost.exception.DeploymentNotFoundException;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.repository.DeploymentRepository;
import com.azhost.repository.ProjectBuildRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentService.class);

    private final ProjectRepository projectRepository;
    private final ProjectBuildRepository projectBuildRepository;
    private final DeploymentRepository deploymentRepository;
    private final UserRepository userRepository;
    private final BuildWorkspaceManager buildWorkspaceManager;
    private final DeploymentValidator deploymentValidator;
    private final DeploymentManager deploymentManager;
    private final AuditLogService auditLogService;

    @Value("${azhost.server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    public DeploymentService(
            ProjectRepository projectRepository,
            ProjectBuildRepository projectBuildRepository,
            DeploymentRepository deploymentRepository,
            UserRepository userRepository,
            BuildWorkspaceManager buildWorkspaceManager,
            DeploymentValidator deploymentValidator,
            DeploymentManager deploymentManager,
            AuditLogService auditLogService
    ) {
        this.projectRepository = projectRepository;
        this.projectBuildRepository = projectBuildRepository;
        this.deploymentRepository = deploymentRepository;
        this.userRepository = userRepository;
        this.buildWorkspaceManager = buildWorkspaceManager;
        this.deploymentValidator = deploymentValidator;
        this.deploymentManager = deploymentManager;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DeploymentResponseDto createDeployment(UUID projectId, CreateDeploymentRequest request, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        ProjectBuildEntity build = projectBuildRepository.findByIdAndProjectId(request.getBuildId(), projectId)
                .orElseThrow(() -> new BuildNotFoundException("Build not found with ID: " + request.getBuildId()));

        Path artifactsRoot = buildWorkspaceManager.getArtifactsRoot();
        Path artifactZipPath = artifactsRoot.resolve(build.getArtifactId() + ".zip").normalize();

        try {
            deploymentValidator.validateBuildForDeployment(project, build, artifactZipPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Artifact validation error: " + e.getMessage(), e);
        }

        deploymentManager.registerActiveDeployment(projectId, UUID.randomUUID());

        DeploymentEntity deploymentEntity = new DeploymentEntity(project, build, build.getArtifactId());
        DeploymentEntity savedEntity = deploymentRepository.save(deploymentEntity);
        auditLogService.log(user, project, "DEPLOYMENT_CREATED", "Deployment", savedEntity.getId().toString(), "SUCCESS", "Created deployment from build " + build.getId());

        String generatedUrl = serverBaseUrl + "/api/deployments/" + savedEntity.getId() + "/files/index.html";

        deploymentManager.submitDeploymentTask(
                savedEntity,
                artifactZipPath,
                generatedUrl,
                () -> setActiveDeploymentForProject(project.getId(), savedEntity.getId())
        );

        return new DeploymentResponseDto(savedEntity, false);
    }

    @Transactional(readOnly = true)
    public DeploymentListResponseDto getDeploymentsForProject(UUID projectId, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        UUID activeId = project.getActiveDeployment() != null ? project.getActiveDeployment().getId() : null;

        List<DeploymentResponseDto> list = deploymentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId()).stream()
                .map(entity -> new DeploymentResponseDto(entity, entity.getId().equals(activeId)))
                .collect(Collectors.toList());

        return new DeploymentListResponseDto(list);
    }

    @Transactional(readOnly = true)
    public DeploymentResponseDto getDeploymentById(UUID projectId, UUID deploymentId, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        DeploymentEntity entity = deploymentRepository.findByIdAndProjectId(deploymentId, projectId)
                .orElseThrow(() -> new DeploymentNotFoundException("Deployment not found with ID: " + deploymentId));

        boolean isActive = project.getActiveDeployment() != null && project.getActiveDeployment().getId().equals(entity.getId());
        return new DeploymentResponseDto(entity, isActive);
    }

    @Transactional
    public DeploymentResponseDto cancelDeployment(UUID projectId, UUID deploymentId, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        DeploymentEntity entity = deploymentRepository.findByIdAndProjectId(deploymentId, projectId)
                .orElseThrow(() -> new DeploymentNotFoundException("Deployment not found with ID: " + deploymentId));

        if (!entity.getStatus().isTerminal()) {
            entity.setStatus(DeploymentStatus.CANCELLED);
            entity.setFailedAt(ZonedDateTime.now());
            deploymentRepository.save(entity);
            deploymentManager.unregisterActiveDeployment(projectId);
            auditLogService.log(user, project, "DEPLOYMENT_CANCELLED", "Deployment", deploymentId.toString(), "SUCCESS", "Cancelled deployment");
        }

        boolean isActive = project.getActiveDeployment() != null && project.getActiveDeployment().getId().equals(entity.getId());
        return new DeploymentResponseDto(entity, isActive);
    }

    @Transactional
    public DeploymentResponseDto rollbackToDeployment(UUID projectId, UUID deploymentId, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        DeploymentEntity entity = deploymentRepository.findByIdAndProjectId(deploymentId, projectId)
                .orElseThrow(() -> new DeploymentNotFoundException("Deployment not found with ID: " + deploymentId));

        if (entity.getStatus() != DeploymentStatus.SUCCESS) {
            throw new IllegalArgumentException("Cannot rollback to a deployment that was not successful.");
        }

        project.setActiveDeployment(entity);
        projectRepository.save(project);
        auditLogService.log(user, project, "ROLLBACK_PERFORMED", "Deployment", deploymentId.toString(), "SUCCESS", "Rolled back project to deployment " + deploymentId);
        logger.info("Rolled back project '{}' to deployment ID: {}", project.getName(), entity.getId());

        return new DeploymentResponseDto(entity, true);
    }

    @Transactional
    public synchronized void setActiveDeploymentForProject(UUID projectId, UUID deploymentId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        DeploymentEntity deployment = deploymentRepository.findById(deploymentId).orElse(null);
        if (project != null && deployment != null && deployment.getStatus() == DeploymentStatus.SUCCESS) {
            DeploymentEntity currentActive = project.getActiveDeployment();
            if (currentActive != null) {
                // If candidate deployment's creation time is before the currently active one, reject promotion
                if (deployment.getCreatedAt().isBefore(currentActive.getCreatedAt())) {
                    logger.warn("Prevented stale deployment promotion: Candidate deployment '{}' (created: {}) is older than currently active deployment '{}' (created: {})",
                            deployment.getId(), deployment.getCreatedAt(), currentActive.getId(), currentActive.getCreatedAt());
                    return;
                }
            }
            project.setActiveDeployment(deployment);
            projectRepository.save(project);
            logger.info("Updated active deployment for project '{}' to deployment ID: {}", project.getName(), deploymentId);
        }
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
