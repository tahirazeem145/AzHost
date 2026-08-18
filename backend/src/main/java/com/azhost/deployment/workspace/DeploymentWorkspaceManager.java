package com.azhost.deployment.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Component
public class DeploymentWorkspaceManager {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentWorkspaceManager.class);

    @Value("${azhost.deployment.workspace-root:./deployment-workspaces}")
    private String workspaceRoot;

    @Value("${azhost.deployment.deployment-root:./deployments}")
    private String deploymentRoot;

    public Path getWorkspacesRoot() {
        return Paths.get(workspaceRoot).toAbsolutePath().normalize();
    }

    public Path getDeploymentsRoot() {
        return Paths.get(deploymentRoot).toAbsolutePath().normalize();
    }

    public Path createWorkspace(String deploymentId) throws IOException {
        Path root = getWorkspacesRoot();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        Path workspacePath = root.resolve(deploymentId).normalize();
        if (!workspacePath.startsWith(root)) {
            throw new SecurityException("Invalid deployment workspace path escape attempt: " + deploymentId);
        }

        Files.createDirectories(workspacePath);
        logger.info("Allocated deployment workspace: {}", workspacePath);
        return workspacePath;
    }

    public Path createDeploymentDirectory(String deploymentId) throws IOException {
        Path root = getDeploymentsRoot();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        Path deploymentPath = root.resolve(deploymentId).normalize();
        if (!deploymentPath.startsWith(root)) {
            throw new SecurityException("Invalid deployment directory path escape attempt: " + deploymentId);
        }

        if (Files.exists(deploymentPath)) {
            logger.info("Deployment directory already exists (immutable): {}", deploymentPath);
        } else {
            Files.createDirectories(deploymentPath);
        }
        return deploymentPath;
    }

    public void deleteDirectoryRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            logger.info("Successfully deleted temporary workspace: {}", path);
        } catch (IOException e) {
            logger.warn("Failed to delete directory: {} ({})", path, e.getMessage());
        }
    }
}
