package com.azhost.service;

import com.azhost.config.AzHostBuildProperties;
import com.azhost.build.workspace.BuildWorkspaceManager;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.repository.ProjectBuildRepository;
import com.azhost.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class StorageQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(StorageQuotaService.class);

    private final AzHostBuildProperties buildProperties;
    private final BuildWorkspaceManager workspaceManager;
    private final ProjectBuildRepository buildRepository;
    private final ProjectRepository projectRepository;

    public StorageQuotaService(
            AzHostBuildProperties buildProperties,
            BuildWorkspaceManager workspaceManager,
            ProjectBuildRepository buildRepository,
            ProjectRepository projectRepository
    ) {
        this.buildProperties = buildProperties;
        this.workspaceManager = workspaceManager;
        this.buildRepository = buildRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public boolean reserveSpace(UUID projectId, long expectedBytes) {
        Project project = projectRepository.findAndLockById(projectId).orElse(null);
        if (project == null) {
            return false;
        }

        long currentUsage = getProjectStorageUsage(projectId);
        long reserved = project.getReservedStorageBytes();
        long maxAllowed = buildProperties.getLimits().getPerProject().getMaxArtifactStorageBytes();

        if (currentUsage + reserved + expectedBytes > maxAllowed) {
            logger.warn("[QUOTA] Project {} storage quota check failed. Current: {} bytes, Reserved: {} bytes, Requested: {} bytes, Max: {} bytes",
                    projectId, currentUsage, reserved, expectedBytes, maxAllowed);
            return false;
        }

        project.setReservedStorageBytes(reserved + expectedBytes);
        projectRepository.save(project);
        logger.info("[QUOTA] Reserved {} bytes for project {}. Total reserved: {} bytes",
                expectedBytes, projectId, project.getReservedStorageBytes());
        return true;
    }

    @Transactional
    public void releaseReservation(UUID projectId, long expectedBytes) {
        Project project = projectRepository.findAndLockById(projectId).orElse(null);
        if (project != null) {
            long currentReserved = project.getReservedStorageBytes();
            long newReserved = Math.max(0, currentReserved - expectedBytes);
            project.setReservedStorageBytes(newReserved);
            projectRepository.save(project);
            logger.info("[QUOTA] Released {} bytes reservation for project {}. Remaining reserved: {} bytes",
                    expectedBytes, projectId, newReserved);
        }
    }

    public long getProjectStorageUsage(UUID projectId) {
        Path artifactsRoot = workspaceManager.getArtifactsRoot();
        List<ProjectBuildEntity> builds = buildRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        long total = 0;
        for (ProjectBuildEntity build : builds) {
            if (build.getArtifactId() != null) {
                Path zipPath = artifactsRoot.resolve(build.getArtifactId() + ".zip");
                if (Files.exists(zipPath)) {
                    try {
                        total += Files.size(zipPath);
                    } catch (IOException e) {
                        logger.debug("Failed to read file size for artifact {}: {}", build.getArtifactId(), e.getMessage());
                    }
                }
            }
        }
        return total;
    }
}
