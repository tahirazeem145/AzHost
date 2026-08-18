package com.azhost.deployment;

import com.azhost.build.BuildStatus;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.exception.BuildNotSuccessfulException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DeploymentValidator {

    public void validateBuildForDeployment(Project project, ProjectBuildEntity build, Path zipFilePath) throws IOException {
        if (!build.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Specified build does not belong to project: " + project.getId());
        }

        if (build.getStatus() != BuildStatus.SUCCESS) {
            throw new BuildNotSuccessfulException("Only successful builds can be deployed.");
        }

        if (build.getArtifactId() == null || build.getArtifactId().isBlank()) {
            throw new BuildNotSuccessfulException("Build record has no associated artifact ID.");
        }

        if (!Files.exists(zipFilePath) || !Files.isRegularFile(zipFilePath)) {
            throw new IOException("Artifact archive file missing on server: " + zipFilePath);
        }
    }
}
