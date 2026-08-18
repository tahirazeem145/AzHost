package com.azhost.deployment.artifact;

import com.azhost.deployment.security.DeploymentResourceLimits;
import com.azhost.deployment.security.DeploymentSecurityPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipArtifactReader implements ArtifactReader {

    private static final Logger logger = LoggerFactory.getLogger(ZipArtifactReader.class);

    @Override
    public ArtifactMetadata extractArtifact(Path zipFilePath, Path targetWorkspaceDir) throws IOException {
        if (!Files.exists(zipFilePath)) {
            throw new IOException("Artifact archive not found at path: " + zipFilePath);
        }

        long zipSize = Files.size(zipFilePath);
        if (zipSize > DeploymentResourceLimits.MAX_ARTIFACT_SIZE_BYTES) {
            throw new SecurityException("Artifact archive exceeds maximum size limit of 500 MB");
        }

        long fileCount = 0;
        long totalBytes = 0;
        boolean hasIndexHtml = false;

        try (InputStream fis = Files.newInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Reject path traversal, absolute paths, Windows drive paths (C:), UNC paths (\\)
                if (entryName.startsWith("/") || entryName.startsWith("\\") || entryName.contains(":") || entryName.contains("..")) {
                    throw new SecurityException("Illegal ZIP entry path detected in artifact: " + entryName);
                }

                Path resolvedPath = targetWorkspaceDir.resolve(entryName).normalize();
                if (!resolvedPath.startsWith(targetWorkspaceDir)) {
                    throw new SecurityException("ZIP-Slip path traversal attack blocked in artifact entry: " + entryName);
                }

                if (DeploymentSecurityPolicy.isForbiddenFileExtension(entryName)) {
                    throw new SecurityException("Forbidden server-side executable file detected in artifact: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    fileCount++;
                    if (fileCount > DeploymentResourceLimits.MAX_FILE_COUNT) {
                        throw new SecurityException("Artifact exceeds maximum file count limit of " + DeploymentResourceLimits.MAX_FILE_COUNT);
                    }

                    if (resolvedPath.getFileName() != null && resolvedPath.getFileName().toString().equalsIgnoreCase("index.html")) {
                        hasIndexHtml = true;
                    }

                    if (resolvedPath.getParent() != null && !Files.exists(resolvedPath.getParent())) {
                        Files.createDirectories(resolvedPath.getParent());
                    }

                    byte[] buffer = new byte[8192];
                    int len;
                    long entrySize = 0;
                    try (var os = Files.newOutputStream(resolvedPath)) {
                        while ((len = zis.read(buffer)) > 0) {
                            entrySize += len;
                            totalBytes += len;

                            if (entrySize > DeploymentResourceLimits.MAX_FILE_SIZE_BYTES) {
                                throw new SecurityException("Single file in artifact exceeds 100 MB limit: " + entryName);
                            }
                            if (totalBytes > DeploymentResourceLimits.MAX_EXTRACTED_SIZE_BYTES) {
                                throw new SecurityException("Artifact uncompressed size exceeds limit of 1 GB");
                            }
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        logger.info("Successfully extracted artifact into workspace '{}': {} files, {} bytes",
                targetWorkspaceDir.getFileName(), fileCount, totalBytes);

        return new ArtifactMetadata(zipFilePath, fileCount, totalBytes, hasIndexHtml);
    }
}
