package com.azhost.source;

import com.azhost.entity.ProjectSourceType;

import java.nio.file.Path;

public class SourceAcquisitionResult {

    private final Path sourceDirectory;
    private final ProjectSourceType sourceType;
    private final long totalFileCount;
    private final long totalSizeBytes;

    public SourceAcquisitionResult(Path sourceDirectory, ProjectSourceType sourceType, long totalFileCount, long totalSizeBytes) {
        this.sourceDirectory = sourceDirectory;
        this.sourceType = sourceType;
        this.totalFileCount = totalFileCount;
        this.totalSizeBytes = totalSizeBytes;
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public ProjectSourceType getSourceType() {
        return sourceType;
    }

    public long getTotalFileCount() {
        return totalFileCount;
    }

    public long getTotalSizeBytes() {
        return totalSizeBytes;
    }
}
