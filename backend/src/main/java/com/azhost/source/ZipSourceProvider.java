package com.azhost.source;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectSourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipSourceProvider implements SourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(ZipSourceProvider.class);

    public static final long MAX_ZIP_FILE_COUNT = 10_000;
    public static final long MAX_UNCOMPRESSED_SIZE_BYTES = 500L * 1024 * 1024; // 500 MB

    @Override
    public boolean supports(Project project) {
        return project.getSourceType() == ProjectSourceType.ZIP;
    }

    @Override
    public SourceAcquisitionResult acquireSource(Project project, Path targetWorkspaceDir) throws IOException {
        throw new UnsupportedOperationException("Use acquireSourceFromStream to extract ZIP archives safely.");
    }

    public SourceAcquisitionResult extractZipStream(InputStream zipInputStream, Path targetWorkspaceDir) throws IOException {
        long fileCount = 0;
        long totalBytes = 0;

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                String entryName = entry.getName();

                // Reject absolute paths, Windows drive paths (C:), UNC paths (\\), and path traversal
                if (entryName.startsWith("/") || entryName.startsWith("\\") || entryName.contains(":") || entryName.contains("..")) {
                    throw new SecurityException("Illegal ZIP entry path detected: " + entryName);
                }

                Path resolvedPath = targetWorkspaceDir.resolve(entryName).normalize();
                if (!resolvedPath.startsWith(targetWorkspaceDir)) {
                    throw new SecurityException("ZIP Slip path traversal attack blocked for entry: " + entryName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    fileCount++;
                    if (fileCount > MAX_ZIP_FILE_COUNT) {
                        throw new SecurityException("ZIP archive exceeds maximum file count limit of " + MAX_ZIP_FILE_COUNT);
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
                            if (totalBytes > MAX_UNCOMPRESSED_SIZE_BYTES) {
                                throw new SecurityException("ZIP archive uncompressed size exceeds limit of 500 MB");
                            }
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        logger.info("Successfully extracted ZIP archive into workspace '{}': {} files, {} bytes",
                targetWorkspaceDir.getFileName(), fileCount, totalBytes);

        return new SourceAcquisitionResult(targetWorkspaceDir, ProjectSourceType.ZIP, fileCount, totalBytes);
    }
}
