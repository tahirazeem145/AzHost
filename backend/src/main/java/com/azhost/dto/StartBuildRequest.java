package com.azhost.dto;

public class StartBuildRequest {

    private String targetBranch;

    public StartBuildRequest() {}

    public StartBuildRequest(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }
}
