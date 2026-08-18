package com.azhost.source;

import com.azhost.entity.Project;

import java.io.IOException;
import java.nio.file.Path;

public interface SourceProvider {

    boolean supports(Project project);

    SourceAcquisitionResult acquireSource(Project project, Path targetWorkspaceDir) throws IOException;
}
