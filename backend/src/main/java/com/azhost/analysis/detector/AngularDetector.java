package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AngularDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        boolean hasAngularJson = source.fileExists("angular.json");

        if (pkgJsonOpt.isEmpty() && !hasAngularJson) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        List<String> evidence = new ArrayList<>();
        boolean hasAngularDep = false;

        if (pkgJsonOpt.isPresent()) {
            String pkgJson = pkgJsonOpt.get();
            if (pkgJson.contains("\"@angular/core\"")) {
                hasAngularDep = true;
                evidence.add("package.json contains '@angular/core' dependency");
            }
        }

        if (hasAngularJson) {
            evidence.add("angular.json configuration file exists");
        }

        if (hasAngularDep && hasAngularJson) {
            return new FrameworkDetectionResult(true, ProjectFramework.ANGULAR, "Angular CLI", DetectionConfidence.HIGH, evidence);
        } else if (hasAngularDep || hasAngularJson) {
            return new FrameworkDetectionResult(true, ProjectFramework.ANGULAR, "Angular CLI", DetectionConfidence.MEDIUM, evidence);
        }

        return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
    }
}
