package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ReactViteDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isEmpty()) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        String pkgJson = pkgJsonOpt.get();
        List<String> evidence = new ArrayList<>();

        boolean hasReact = pkgJson.contains("\"react\"");
        boolean hasNext = pkgJson.contains("\"next\"");
        boolean hasCra = pkgJson.contains("\"react-scripts\"");
        boolean hasViteDep = pkgJson.contains("\"vite\"");
        boolean hasViteConfig = source.fileExists("vite.config.js") || source.fileExists("vite.config.ts") || source.fileExists("vite.config.mjs") || source.fileExists("vite.config.cjs");

        if (!hasReact || hasNext || hasCra) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        evidence.add("package.json contains 'react' dependency");
        if (hasViteDep) {
            evidence.add("package.json contains 'vite' dependency");
        }
        if (hasViteConfig) {
            evidence.add("Vite configuration file exists");
        }

        if (hasViteDep || hasViteConfig) {
            return new FrameworkDetectionResult(true, ProjectFramework.REACT, "Vite", DetectionConfidence.HIGH, evidence);
        }

        return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
    }
}
