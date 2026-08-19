package com.azhost.service;

import com.azhost.build.workspace.BuildWorkspaceManager;
import com.azhost.config.AzHostBuildProperties;
import com.azhost.entity.DeploymentEntity;
import com.azhost.entity.Project;
import com.azhost.repository.DeploymentRepository;
import com.azhost.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ArtifactRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRetentionService.class);

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final BuildWorkspaceManager workspaceManager;
    private final AzHostBuildProperties properties;

    public ArtifactRetentionService(
            ProjectRepository projectRepository,
            DeploymentRepository deploymentRepository,
            BuildWorkspaceManager workspaceManager,
            AzHostBuildProperties properties
    ) {
        this.projectRepository = projectRepository;
        this.deploymentRepository = deploymentRepository;
        this.workspaceManager = workspaceManager;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 600000) // run every 10 minutes
    @Transactional
    public void cleanupOldArtifacts() {
        logger.info("[RETENTION SERVICE] Starting artifact retention cleanup...");
        List<Project> projects = projectRepository.findAll();
        int maxHistory = properties.getDeployment().getRetention().getMaxDeploymentsPerProject();

        for (Project project : projects) {
            List<DeploymentEntity> deployments = deploymentRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());
            if (deployments.size() <= maxHistory) {
                continue;
            }

            DeploymentEntity active = project.getActiveDeployment();

            for (int i = maxHistory; i < deployments.size(); i++) {
                DeploymentEntity dep = deployments.get(i);

                // Safe guards: never delete active deployment
                if (active != null && dep.getId().equals(active.getId())) {
                    continue;
                }

                // Delete physical files
                if (dep.getDeploymentPath() != null) {
                    Path path = Paths.get(dep.getDeploymentPath());
                    if (Files.exists(path)) {
                        try {
                            FileSystemUtils.deleteRecursively(path);
                            logger.info("[RETENTION SERVICE] Cleaned up deployment directory: {}", path);
                        } catch (IOException e) {
                            logger.warn("[RETENTION SERVICE] Failed to delete deployment directory: {}, error: {}", path, e.getMessage());
                        }
                    }
                    dep.setDeploymentPath(null); // clear directory path to mark as pruned
                    deploymentRepository.save(dep);
                }

                // Delete build zip artifact
                if (dep.getBuild() != null && dep.getBuild().getArtifactId() != null) {
                    Path zipPath = workspaceManager.getArtifactsRoot().resolve(dep.getBuild().getArtifactId() + ".zip");
                    if (Files.exists(zipPath)) {
                        try {
                            Files.delete(zipPath);
                            logger.info("[RETENTION SERVICE] Cleaned up ZIP artifact: {}", zipPath);
                        } catch (IOException e) {
                            logger.warn("[RETENTION SERVICE] Failed to delete ZIP artifact: {}, error: {}", zipPath, e.getMessage());
                        }
                    }
                }
            }
        }
    }
}
