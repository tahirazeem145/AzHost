package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class StaticSiteDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        boolean hasIndexHtml = source.fileExists("index.html");
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");

        if (!hasIndexHtml) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        List<String> evidence = new ArrayList<>();
        evidence.add("index.html exists in root directory");

        if (pkgJsonOpt.isPresent()) {
            String pkgJson = pkgJsonOpt.get();
            if (pkgJson.contains("\"react\"") || pkgJson.contains("\"vue\"") || pkgJson.contains("\"next\"") || pkgJson.contains("\"@angular/core\"")) {
                // Framework present - not purely static HTML
                return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
            }
            evidence.add("package.json contains no framework dependencies");
        } else {
            evidence.add("No package.json manifest detected");
        }

        return new FrameworkDetectionResult(true, ProjectFramework.STATIC, "Static HTML", DetectionConfidence.HIGH, evidence);
    }
}
