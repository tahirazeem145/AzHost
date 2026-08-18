package com.azhost.source;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LocalSourceProvider implements SourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(LocalSourceProvider.class);

    @Value("${azhost.analysis.allowed-root-dir:./test-fixtures}")
    private String allowedRootDir;

    @Override
    public boolean supports(Project project) {
        return project.getSourceType() == ProjectSourceType.LOCAL;
    }

    @Override
    public SourceAcquisitionResult acquireSource(Project project, Path targetWorkspaceDir) throws IOException {
        Path baseRoot = Paths.get(allowedRootDir).normalize().toAbsolutePath();
        Path candidatePathBySlug = baseRoot.resolve(project.getSlug()).normalize();
        Path candidatePathById = baseRoot.resolve(project.getId().toString()).normalize();

        Path sourceDir = candidatePathBySlug;
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            sourceDir = candidatePathById;
        }

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IOException("Local source directory not found for project '" + project.getName() + "'");
        }

        Path realSourceDir = sourceDir.toRealPath();
        if (!realSourceDir.startsWith(baseRoot.toRealPath())) {
            throw new SecurityException("Local source path traversal attempt detected outside allowed root directory");
        }

        AtomicLong fileCount = new AtomicLong(0);
        AtomicLong totalBytes = new AtomicLong(0);

        Files.walkFileTree(realSourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                if (name.equals("node_modules") || name.equals(".git") || name.equals("dist") || name.equals("build") || name.equals(".next")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path rel = realSourceDir.relativize(dir);
                Path destDir = targetWorkspaceDir.resolve(rel);
                if (!Files.exists(destDir)) {
                    Files.createDirectories(destDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = realSourceDir.relativize(file);
                Path destFile = targetWorkspaceDir.resolve(rel);
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                fileCount.incrementAndGet();
                totalBytes.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });

        logger.info("Successfully copied local source into workspace '{}': {} files, {} bytes",
                targetWorkspaceDir.getFileName(), fileCount.get(), totalBytes.get());

        return new SourceAcquisitionResult(targetWorkspaceDir, ProjectSourceType.LOCAL, fileCount.get(), totalBytes.get());
    }
}
