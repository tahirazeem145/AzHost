package com.azhost.analysis.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalDirectorySourceReader implements ProjectSourceReader {

    private static final Logger logger = LoggerFactory.getLogger(LocalDirectorySourceReader.class);

    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024; // 500 KB
    private static final int MAX_FILE_COUNT = 100;

    private static final Set<String> ALLOWED_MANIFEST_FILES = Set.of(
            "package.json", "package-lock.json", "pnpm-lock.yaml", "yarn.lock", "bun.lock", "bun.lockb",
            "vite.config.js", "vite.config.ts", "vite.config.mjs", "vite.config.cjs",
            "next.config.js", "next.config.ts", "next.config.mjs",
            "angular.json", "vue.config.js",
            ".nvmrc", ".node-version", "tsconfig.json", "index.html"
    );

    private static final Set<String> EXCLUDED_DIRS = Set.of(
            "node_modules", ".git", "dist", "build", ".next", "target"
    );

    private final Path rootPath;
    private final Path realRootPath;

    public LocalDirectorySourceReader(Path rootPath) {
        this.rootPath = rootPath != null ? rootPath.normalize() : null;
        Path tempReal = null;
        if (this.rootPath != null && Files.exists(this.rootPath)) {
            try {
                tempReal = this.rootPath.toRealPath();
            } catch (IOException e) {
                logger.warn("Could not resolve real path for root: {}", this.rootPath);
            }
        }
        this.realRootPath = tempReal;
    }

    @Override
    public boolean exists() {
        return realRootPath != null && Files.exists(realRootPath) && Files.isDirectory(realRootPath);
    }

    @Override
    public boolean fileExists(String relativePath) {
        Optional<Path> resolved = resolveSafePath(relativePath);
        return resolved.isPresent() && Files.exists(resolved.get()) && Files.isRegularFile(resolved.get());
    }

    @Override
    public Optional<String> readFileContent(String relativePath) {
        Optional<Path> resolved = resolveSafePath(relativePath);
        if (resolved.isEmpty()) {
            return Optional.empty();
        }

        Path path = resolved.get();
        try {
            if (!Files.isRegularFile(path)) {
                return Optional.empty();
            }

            long size = Files.size(path);
            if (size > MAX_FILE_SIZE_BYTES) {
                logger.warn("File {} exceeds max size limit of {} KB (size: {} bytes). Skipping content read.", relativePath, MAX_FILE_SIZE_BYTES / 1024, size);
                return Optional.empty();
            }

            String content = Files.readString(path);
            return Optional.of(content);
        } catch (IOException e) {
            logger.warn("Failed to read file content for relative path: {}", relativePath);
            return Optional.empty();
        }
    }

    @Override
    public List<String> listRootFiles() {
        if (!exists()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(realRootPath)) {
            List<Path> paths = stream.limit(MAX_FILE_COUNT).collect(Collectors.toList());

            for (Path path : paths) {
                String fileName = path.getFileName().toString();
                if (EXCLUDED_DIRS.contains(fileName.toLowerCase(Locale.ENGLISH))) {
                    continue;
                }

                if (Files.isRegularFile(path) && ALLOWED_MANIFEST_FILES.contains(fileName)) {
                    result.add(fileName);
                }
            }
        } catch (IOException e) {
            logger.warn("Error listing root files for path: {}", realRootPath);
        }

        return result;
    }

    @Override
    public Path getRootPath() {
        return realRootPath != null ? realRootPath : rootPath;
    }

    private Optional<Path> resolveSafePath(String relativePath) {
        if (!exists() || relativePath == null || relativePath.isBlank()) {
            return Optional.empty();
        }

        // Reject explicit path traversal attempts
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            logger.warn("Path traversal attempt blocked for relative path: {}", relativePath);
            return Optional.empty();
        }

        try {
            Path target = realRootPath.resolve(relativePath).normalize();

            // Verification 1: Is it within root path?
            if (!target.startsWith(realRootPath)) {
                logger.warn("Path escape attempt blocked: {}", relativePath);
                return Optional.empty();
            }

            // Verification 2: Check real canonical path if file exists
            if (Files.exists(target)) {
                Path realTarget = target.toRealPath();
                if (!realTarget.startsWith(realRootPath)) {
                    logger.warn("Symlink/Junction escape attempt blocked: {}", relativePath);
                    return Optional.empty();
                }
                return Optional.of(realTarget);
            }

            return Optional.of(target);
        } catch (IOException e) {
            logger.warn("Exception resolving safe path: {}", relativePath);
            return Optional.empty();
        }
    }
}
