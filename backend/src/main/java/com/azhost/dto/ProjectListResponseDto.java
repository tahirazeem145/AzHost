package com.azhost.dto;

import java.util.List;

public class ProjectListResponseDto {

    private List<ProjectResponseDto> projects;
    private long totalCount;

    public ProjectListResponseDto() {}

    public ProjectListResponseDto(List<ProjectResponseDto> projects, long totalCount) {
        this.projects = projects;
        this.totalCount = totalCount;
    }

    public List<ProjectResponseDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectResponseDto> projects) {
        this.projects = projects;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
