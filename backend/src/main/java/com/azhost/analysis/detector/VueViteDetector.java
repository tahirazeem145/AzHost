package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class VueViteDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isEmpty()) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        String pkgJson = pkgJsonOpt.get();
        List<String> evidence = new ArrayList<>();

        boolean hasVue = pkgJson.contains("\"vue\"");
        boolean hasViteDep = pkgJson.contains("\"vite\"");
        boolean hasViteConfig = source.fileExists("vite.config.js") || source.fileExists("vite.config.ts") || source.fileExists("vite.config.mjs");
        boolean hasVueConfig = source.fileExists("vue.config.js");

        if (!hasVue) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        evidence.add("package.json contains 'vue' dependency");
        if (hasViteDep || hasViteConfig) {
            evidence.add("Vite configuration or dependency detected");
            return new FrameworkDetectionResult(true, ProjectFramework.VUE, "Vite", DetectionConfidence.HIGH, evidence);
        } else if (hasVueConfig) {
            evidence.add("vue.config.js configuration file exists");
            return new FrameworkDetectionResult(true, ProjectFramework.VUE, "Vue CLI", DetectionConfidence.HIGH, evidence);
        }

        return new FrameworkDetectionResult(true, ProjectFramework.VUE, "Vue", DetectionConfidence.MEDIUM, evidence);
    }
}
