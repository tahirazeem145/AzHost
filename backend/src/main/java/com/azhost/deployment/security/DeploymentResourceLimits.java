package com.azhost.deployment.security;

public class DeploymentResourceLimits {

    public static final long MAX_ARTIFACT_SIZE_BYTES = 500L * 1024 * 1024;    // 500 MB
    public static final long MAX_EXTRACTED_SIZE_BYTES = 1024L * 1024 * 1024;  // 1 GB
    public static final long MAX_FILE_SIZE_BYTES = 100L * 1024 * 1024;        // 100 MB
    public static final long MAX_FILE_COUNT = 20_000;

    public static final int MAX_GLOBAL_CONCURRENT_DEPLOYMENTS = 2;
    public static final int MAX_PER_PROJECT_CONCURRENT_DEPLOYMENTS = 1;

    private DeploymentResourceLimits() {}
}
