package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ReactCraDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isEmpty()) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        String pkgJson = pkgJsonOpt.get();
        List<String> evidence = new ArrayList<>();

        boolean hasReact = pkgJson.contains("\"react\"");
        boolean hasCra = pkgJson.contains("\"react-scripts\"");

        if (hasReact && hasCra) {
            evidence.add("package.json contains 'react' dependency");
            evidence.add("package.json contains 'react-scripts' dependency");
            return new FrameworkDetectionResult(true, ProjectFramework.REACT, "Create React App", DetectionConfidence.HIGH, evidence);
        }

        return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
    }
}
