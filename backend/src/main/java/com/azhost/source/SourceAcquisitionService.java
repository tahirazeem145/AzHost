package com.azhost.source;

import com.azhost.entity.Project;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class SourceAcquisitionService {

    private final List<SourceProvider> sourceProviders;

    public SourceAcquisitionService(List<SourceProvider> sourceProviders) {
        this.sourceProviders = sourceProviders;
    }

    public SourceAcquisitionResult acquireSource(Project project, Path targetWorkspaceDir) throws IOException {
        SourceProvider provider = sourceProviders.stream()
                .filter(p -> p.supports(project))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported source type: " + project.getSourceType()));

        return provider.acquireSource(project, targetWorkspaceDir);
    }
}
