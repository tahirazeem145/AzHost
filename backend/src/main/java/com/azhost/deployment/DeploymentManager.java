package com.azhost.deployment;

import com.azhost.deployment.artifact.ArtifactMetadata;
import com.azhost.deployment.artifact.ArtifactReader;
import com.azhost.deployment.publisher.StaticFilePublisher;
import com.azhost.deployment.security.DeploymentResourceLimits;
import com.azhost.deployment.workspace.DeploymentCleanupService;
import com.azhost.deployment.workspace.DeploymentWorkspaceManager;
import com.azhost.entity.DeploymentEntity;
import com.azhost.exception.DeploymentAlreadyInProgressException;
import com.azhost.repository.DeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DeploymentManager {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentManager.class);

    private final DeploymentRepository deploymentRepository;
    private final DeploymentWorkspaceManager workspaceManager;
    private final DeploymentCleanupService cleanupService;
    private final ArtifactReader artifactReader;
    private final StaticFilePublisher staticFilePublisher;

    private final ConcurrentHashMap<UUID, UUID> activeProjectDeployments = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(DeploymentResourceLimits.MAX_GLOBAL_CONCURRENT_DEPLOYMENTS);

    public DeploymentManager(
            DeploymentRepository deploymentRepository,
            DeploymentWorkspaceManager workspaceManager,
            DeploymentCleanupService cleanupService,
            ArtifactReader artifactReader,
            StaticFilePublisher staticFilePublisher
    ) {
        this.deploymentRepository = deploymentRepository;
        this.workspaceManager = workspaceManager;
        this.cleanupService = cleanupService;
        this.artifactReader = artifactReader;
        this.staticFilePublisher = staticFilePublisher;
    }

    public synchronized void registerActiveDeployment(UUID projectId, UUID deploymentId) {
        if (activeProjectDeployments.containsKey(projectId)) {
            throw new DeploymentAlreadyInProgressException("A deployment is already in progress for project ID: " + projectId);
        }
        activeProjectDeployments.put(projectId, deploymentId);
    }

    public synchronized void unregisterActiveDeployment(UUID projectId) {
        activeProjectDeployments.remove(projectId);
    }

    public synchronized void reset() {
        activeProjectDeployments.clear();
    }

    public boolean isDeploymentInProgressForProject(UUID projectId) {
        return activeProjectDeployments.containsKey(projectId);
    }

    public void submitDeploymentTask(
            DeploymentEntity deploymentEntity,
            Path artifactZipPath,
            String generatedUrl,
            Runnable onSuccessCallback
    ) {
        executorService.submit(() -> {
            String deploymentIdStr = deploymentEntity.getId().toString();
            Path tempWorkspacePath = null;
            try {
                // State 1: PREPARING
                updateStatus(deploymentEntity, DeploymentStatus.PREPARING, null);
                tempWorkspacePath = workspaceManager.createWorkspace(deploymentIdStr);

                // State 2: EXTRACTING
                updateStatus(deploymentEntity, DeploymentStatus.EXTRACTING, null);
                ArtifactMetadata metadata = artifactReader.extractArtifact(artifactZipPath, tempWorkspacePath);

                // State 3: VALIDATING
                updateStatus(deploymentEntity, DeploymentStatus.VALIDATING, null);
                if (!metadata.hasIndexHtml()) {
                    throw new IllegalArgumentException("Static artifact validation failed: index.html is missing at the root of the published artifact.");
                }

                // State 4: PUBLISHING
                updateStatus(deploymentEntity, DeploymentStatus.PUBLISHING, null);
                Path immutableTargetDir = workspaceManager.createDeploymentDirectory(deploymentIdStr);
                staticFilePublisher.publishStaticSite(tempWorkspacePath, immutableTargetDir);

                // State 5: SUCCESS
                deploymentEntity.setStatus(DeploymentStatus.SUCCESS);
                deploymentEntity.setDeploymentPath(immutableTargetDir.toString());
                deploymentEntity.setDeploymentUrl(generatedUrl);
                deploymentEntity.setPublishedAt(ZonedDateTime.now());
                deploymentRepository.save(deploymentEntity);

                if (onSuccessCallback != null) {
                    onSuccessCallback.run();
                }

                logger.info("Deployment {} published successfully to URL: {}", deploymentEntity.getId(), generatedUrl);

            } catch (Exception e) {
                logger.error("Deployment failed for ID {}: {}", deploymentEntity.getId(), e.getMessage(), e);
                deploymentEntity.setStatus(DeploymentStatus.FAILED);
                deploymentEntity.setErrorMessage(e.getMessage());
                deploymentEntity.setFailedAt(ZonedDateTime.now());
                deploymentRepository.save(deploymentEntity);
            } finally {
                unregisterActiveDeployment(deploymentEntity.getProject().getId());
                if (tempWorkspacePath != null) {
                    cleanupService.cleanupWorkspace(tempWorkspacePath);
                }
            }
        });
    }

    private void updateStatus(DeploymentEntity entity, DeploymentStatus status, String error) {
        entity.setStatus(status);
        if (error != null) {
            entity.setErrorMessage(error);
        }
        deploymentRepository.save(entity);
    }
}
