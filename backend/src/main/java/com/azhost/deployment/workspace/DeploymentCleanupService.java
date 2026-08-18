package com.azhost.deployment.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class DeploymentCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentCleanupService.class);

    private final DeploymentWorkspaceManager workspaceManager;

    public DeploymentCleanupService(DeploymentWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public void cleanupWorkspace(Path workspacePath) {
        workspaceManager.deleteDirectoryRecursively(workspacePath);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOrphanWorkspacesOnStartup() {
        Path root = workspaceManager.getWorkspacesRoot();
        if (!Files.exists(root)) return;

        logger.info("Checking for orphan deployment workspaces to clean up on startup...");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    logger.info("Cleaning orphan deployment workspace: {}", entry.getFileName());
                    workspaceManager.deleteDirectoryRecursively(entry);
                }
            }
        } catch (IOException e) {
            logger.error("Error during orphan deployment workspace cleanup on startup: {}", e.getMessage(), e);
        }
    }
}
