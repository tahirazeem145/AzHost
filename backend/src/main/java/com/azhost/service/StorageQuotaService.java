package com.azhost.service;

import com.azhost.config.AzHostBuildProperties;
import com.azhost.build.workspace.BuildWorkspaceManager;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.repository.ProjectBuildRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StorageQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(StorageQuotaService.class);

    private final AzHostBuildProperties buildProperties;
    private final BuildWorkspaceManager workspaceManager;
    private final ProjectBuildRepository buildRepository;

    private final Map<UUID, AtomicLong> reservedBytes = new ConcurrentHashMap<>();

    public StorageQuotaService(
            AzHostBuildProperties buildProperties,
            BuildWorkspaceManager workspaceManager,
            ProjectBuildRepository buildRepository
    ) {
        this.buildProperties = buildProperties;
        this.workspaceManager = workspaceManager;
        this.buildRepository = buildRepository;
    }

    public synchronized boolean reserveSpace(UUID projectId, long expectedBytes) {
        long currentUsage = getProjectStorageUsage(projectId);
        long reserved = reservedBytes.computeIfAbsent(projectId, k -> new AtomicLong(0)).get();
        long maxAllowed = buildProperties.getLimits().getPerProject().getMaxArtifactStorageBytes();

        if (currentUsage + reserved + expectedBytes > maxAllowed) {
            logger.warn("[QUOTA] Project {} storage quota check failed. Current: {} bytes, Reserved: {} bytes, Requested: {} bytes, Max: {} bytes",
                    projectId, currentUsage, reserved, expectedBytes, maxAllowed);
            return false;
        }

        reservedBytes.get(projectId).addAndGet(expectedBytes);
        logger.info("[QUOTA] Reserved {} bytes for project {}. Total reserved: {} bytes",
                expectedBytes, projectId, reservedBytes.get(projectId).get());
        return true;
    }

    public synchronized void releaseReservation(UUID projectId, long expectedBytes) {
        AtomicLong reserved = reservedBytes.get(projectId);
        if (reserved != null) {
            long remaining = reserved.addAndGet(-expectedBytes);
            if (remaining <= 0) {
                reservedBytes.remove(projectId);
            }
            logger.info("[QUOTA] Released {} bytes reservation for project {}. Remaining reserved: {} bytes",
                    expectedBytes, projectId, remaining);
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
