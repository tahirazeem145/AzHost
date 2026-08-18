package com.azhost.deployment.artifact;

import java.nio.file.Path;

public class ArtifactMetadata {

    private final Path artifactZipPath;
    private final long totalFileCount;
    private final long totalSizeBytes;
    private final boolean hasIndexHtml;

    public ArtifactMetadata(Path artifactZipPath, long totalFileCount, long totalSizeBytes, boolean hasIndexHtml) {
        this.artifactZipPath = artifactZipPath;
        this.totalFileCount = totalFileCount;
        this.totalSizeBytes = totalSizeBytes;
        this.hasIndexHtml = hasIndexHtml;
    }

    public Path getArtifactZipPath() {
        return artifactZipPath;
    }

    public long getTotalFileCount() {
        return totalFileCount;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public boolean hasIndexHtml() {
        return hasIndexHtml;
    }
}
