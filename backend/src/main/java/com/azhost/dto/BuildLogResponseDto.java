package com.azhost.dto;

import com.azhost.build.BuildStatus;

import java.util.List;
import java.util.UUID;

public class BuildLogResponseDto {

    private UUID buildId;
    private BuildStatus status;
    private List<String> logs;
    private boolean truncated;

    public BuildLogResponseDto() {}

    public BuildLogResponseDto(UUID buildId, BuildStatus status, List<String> logs, boolean truncated) {
        this.buildId = buildId;
        this.status = status;
        this.logs = logs;
        this.truncated = truncated;
    }

    public UUID getBuildId() { return buildId; }
    public void setBuildId(UUID buildId) { this.buildId = buildId; }

    public BuildStatus getStatus() { return status; }
    public void setStatus(BuildStatus status) { this.status = status; }

    public List<String> getLogs() { return logs; }
    public void setLogs(List<String> logs) { this.logs = logs; }

    public boolean isTruncated() { return truncated; }
    public void setTruncated(boolean truncated) { this.truncated = truncated; }
}
