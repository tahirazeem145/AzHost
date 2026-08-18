package com.azhost.controller;

import com.azhost.deployment.security.DeploymentSecurityPolicy;
import com.azhost.deployment.workspace.DeploymentWorkspaceManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/deployments")
public class StaticHostingController {

    private static final Logger logger = LoggerFactory.getLogger(StaticHostingController.class);

    private final DeploymentWorkspaceManager workspaceManager;

    public StaticHostingController(DeploymentWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @GetMapping("/{deploymentId}/files/**")
    public ResponseEntity<Resource> serveStaticFile(
            @PathVariable UUID deploymentId,
            HttpServletRequest request
    ) {
        Path deploymentsRoot = workspaceManager.getDeploymentsRoot();
        Path deploymentDir = deploymentsRoot.resolve(deploymentId.toString()).normalize();

        if (!Files.exists(deploymentDir) || !Files.isDirectory(deploymentDir)) {
            logger.warn("Deployment directory not found: {}", deploymentDir);
            return ResponseEntity.notFound().build();
        }

        String fullPath = request.getRequestURI();
        String prefix = "/api/deployments/" + deploymentId + "/files/";
        String relativePath = fullPath.startsWith(prefix) ? fullPath.substring(prefix.length()) : "";

        if (relativePath.isBlank()) {
            relativePath = "index.html";
        }

        // Canonical containment check against path traversal
        Path targetFile = deploymentDir.resolve(relativePath).normalize();
        if (!targetFile.startsWith(deploymentDir)) {
            logger.warn("Path traversal attack blocked in static file request: {}", relativePath);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            // SPA fallback: Return index.html if file doesn't exist
            Path indexFallback = deploymentDir.resolve("index.html").normalize();
            if (Files.exists(indexFallback)) {
                targetFile = indexFallback;
            } else {
                return ResponseEntity.notFound().build();
            }
        }

        String mimeType = DeploymentSecurityPolicy.getMimeType(targetFile.getFileName().toString());
        Resource resource = new FileSystemResource(targetFile.toFile());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(resource);
    }
}
