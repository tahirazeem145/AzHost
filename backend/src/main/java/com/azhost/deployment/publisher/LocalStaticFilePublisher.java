package com.azhost.deployment.publisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class LocalStaticFilePublisher implements StaticFilePublisher {

    private static final Logger logger = LoggerFactory.getLogger(LocalStaticFilePublisher.class);

    @Override
    public Path publishStaticSite(Path extractedWorkspaceDir, Path targetDeploymentDir) throws IOException {
        if (!Files.exists(extractedWorkspaceDir) || !Files.isDirectory(extractedWorkspaceDir)) {
            throw new IOException("Extracted workspace directory does not exist: " + extractedWorkspaceDir);
        }

        if (!Files.exists(targetDeploymentDir)) {
            Files.createDirectories(targetDeploymentDir);
        }

        // Detect root vs nested output folder containing index.html
        Path publishSourceDir = extractedWorkspaceDir;
        if (!Files.exists(extractedWorkspaceDir.resolve("index.html"))) {
            // Search for single nested folder containing index.html (e.g. dist/ or build/)
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(extractedWorkspaceDir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry) && Files.exists(entry.resolve("index.html"))) {
                        publishSourceDir = entry;
                        logger.info("Detected nested publish root: {}", entry.getFileName());
                        break;
                    }
                }
            }
        }

        if (!Files.exists(publishSourceDir.resolve("index.html"))) {
            throw new IllegalArgumentException("Static deployment validation failed: index.html is missing at the root of the published artifact.");
        }

        Path finalPublishSource = publishSourceDir;
        AtomicBoolean symlinkEscaped = new AtomicBoolean(false);

        Files.walkFileTree(finalPublishSource, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = finalPublishSource.relativize(dir);
                Path destDir = targetDeploymentDir.resolve(rel).normalize();

                if (!destDir.startsWith(targetDeploymentDir)) {
                    symlinkEscaped.set(true);
                    return FileVisitResult.TERMINATE;
                }

                if (!Files.exists(destDir)) {
                    Files.createDirectories(destDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.isSymbolicLink(file)) {
                    Path realPath = file.toRealPath();
                    if (!realPath.startsWith(finalPublishSource.toRealPath())) {
                        logger.warn("Symlink escape attempt detected: {} points to {}", file, realPath);
                        symlinkEscaped.set(true);
                        return FileVisitResult.TERMINATE;
                    }
                }

                Path rel = finalPublishSource.relativize(file);
                Path destFile = targetDeploymentDir.resolve(rel).normalize();

                if (!destFile.startsWith(targetDeploymentDir)) {
                    symlinkEscaped.set(true);
                    return FileVisitResult.TERMINATE;
                }

                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        if (symlinkEscaped.get()) {
            throw new SecurityException("Symlink or path escape attempt detected during static file publication.");
        }

        logger.info("Successfully published static site to immutable deployment dir: {}", targetDeploymentDir);
        return targetDeploymentDir;
    }
}
