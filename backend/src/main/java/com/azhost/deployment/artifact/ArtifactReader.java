package com.azhost.deployment.artifact;

import java.io.IOException;
import java.nio.file.Path;

public interface ArtifactReader {

    ArtifactMetadata extractArtifact(Path zipFilePath, Path targetWorkspaceDir) throws IOException;
}
