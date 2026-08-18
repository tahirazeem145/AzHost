package com.azhost.deployment.publisher;

import java.io.IOException;
import java.nio.file.Path;

public interface StaticFilePublisher {

    Path publishStaticSite(Path extractedWorkspaceDir, Path targetDeploymentDir) throws IOException;
}
